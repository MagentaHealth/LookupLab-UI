(ns story-discovery-frontend.events
  (:require
    [ajax.core :as ajax]
    [goog.functions]
    [re-frame.core :as rf]))

;; -------------------------
;; DB

(rf/reg-sub
  :db
  (fn [db _]
    db))

(rf/reg-event-fx
  :init-db
  (fn [_ _]
    {:db {:view   :search
          :search {:filters  #{"Current Patients" "Non Patients" "Third Parties"}
                   :audience "Current Patients"}}}))

;; -------------------------
;; Views

(rf/reg-sub
  :view
  (fn [db _]
    (:view db)))

(rf/reg-event-db
  :set-view
  (fn [db [_ view]]
    (assoc db :view view)))

;; -------------------------
;; Default/All Triggers

(rf/reg-sub
  :triggers/default
  (fn [db _]
    (get-in db [:triggers :default])))

(rf/reg-sub
  :triggers/all
  (fn [db _]
    (get-in db [:triggers :all])))

(rf/reg-event-db
  :set-triggers
  (fn [db [_ k triggers]]
    (assoc-in db [:triggers k] triggers)))

(rf/reg-event-fx
  :http/get-default-triggers
  (fn [_ [resource-id]]
    {:http {:method      ajax/GET
            :url         "/api/default-triggers"
            :resource-id resource-id
            :on-success  [:set-triggers :default]}}))

(rf/reg-event-fx
  :http/get-all-triggers
  (fn [_ [resource-id]]
    {:http {:method      ajax/GET
            :url         "/api/all-triggers"
            :resource-id resource-id
            :on-success  [:set-triggers :all]}}))

;; -------------------------
;; Searching

(rf/reg-sub
  :search/no-results?
  (fn [db _]
    (boolean (get-in db [:search :no-results]))))

(rf/reg-sub
  :search/results
  (fn [db _]
    (get-in db [:search :results])))

(rf/reg-event-db
  :clear-results
  (fn [db _]
    (update db :search dissoc :results)))

(rf/reg-event-db
  :set-results
  (fn [db [_ results]]
    (if (not-empty results)
      (-> db
          (assoc-in [:search :results] results)
          (update :search dissoc :no-results))
      (-> db
          (assoc-in [:search :no-results] true)
          (update :search dissoc :results)))))

(rf/reg-event-fx
  :http/search-triggers
  (fn [{:keys [db]} [resource-id]]
    (when-let [query (not-empty (get-in db [:search :query]))]
      (let [audience (get-in db [:search :audience])]
        {:http {:method      ajax/GET
                :url         "/api/search"
                :resource-id resource-id
                :params      {:query query :audience audience}
                :on-success  [:set-results]}}))))

(def debounced-search
  (goog.functions.debounce #(rf/dispatch [:http/search-triggers]) 500))

(rf/reg-sub
  :search/query
  (fn [db _]
    (get-in db [:search :query])))

(rf/reg-event-fx
  :set-query
  (fn [{:keys [db]} [_ query]]
    (merge
      {:db (-> db
               (assoc-in [:search :query] query)
               (update :search dissoc :no-results))}
      (if (empty? query)
        {:dispatch [:clear-results]}
        {:side-effect debounced-search}))))

;; -------------------------
;; Audience filter

(rf/reg-sub
  :search/audience
  (fn [db _]
    (get-in db [:search :audience])))

(rf/reg-event-fx
  :set-audience
  (fn [{:keys [db]} [_ audience]]
    (merge
      {:db (assoc-in db [:search :audience] audience)}
      (when (not-empty (get-in db [:search :query]))
        {:dispatch [:http/search-triggers]}))))

;; -------------------------
;; Click through & popups

(rf/reg-sub
  :trigger/message
  (fn [db _]
    (:trigger-message db)))

(rf/reg-event-db
  :trigger/clear-message
  (fn [db _]
    (dissoc db :trigger-message)))

(defn log-click-and-navigate [query audience trigger]
  (let [payload (->> {:query    query
                      :audience audience
                      :trigger  trigger}
                     (clj->js)
                     (.stringify js/JSON))]
    (.sendBeacon
      js/navigator
      (str js/apiURL "/api/log-click-through")
      (js/Blob.
        [payload]
        (clj->js {:type "application/json"}))))
  (rf/dispatch [:trigger/clear-message])
  (set! (. js/location -href) (:destination trigger)))

(defn delay-click-through [query audience trigger]
  (js/setTimeout
    #(log-click-and-navigate query audience trigger)
    5000))

(rf/reg-event-fx
  :select-trigger
  (fn [{:keys [db]} [_ trigger]]
    (let [query    (str (get-in db [:search :query]))
          audience (str (get-in db [:search :audience]))]
      (if-let [message (not-empty (:message trigger))]
        {:db          (assoc db :trigger-message message)
         :side-effect #(delay-click-through query audience trigger)}
        {:side-effect #(log-click-and-navigate query audience trigger)}))))