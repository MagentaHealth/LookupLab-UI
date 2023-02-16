(ns story-discovery-frontend.events
  (:require
    [ajax.core :as ajax]
    [cemerick.url :as url]
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
          :search {:audience        #{"Current Patients" "Non Patients" "Third Parties"}
                   :prompts         ["I want to ask a question about..."
                                     "I want to book an appointment for..."
                                     "I want information about..."
                                     "I want to request..."]
                   :placeholders    ["the status of my referral"
                                     "my anxiety"
                                     "clinic hours"
                                     "a sick note"]
                   :selected-prompt (js/Math.floor (* 4 (js/Math.random)))
                   :prompt-class    "fade-in"}}}))

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
      {:http {:method      ajax/GET
              :url         "/api/search"
              :resource-id resource-id
              :params      {:query query}
              :on-success  [:set-results]}})))

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
    {:db (update-in db [:search :audience] conj audience)}))

(rf/reg-event-fx
  :unset-audience
  (fn [{:keys [db]} [_ audience]]
    {:db (update-in db [:search :audience] disj audience)}))

;; -------------------------
;; Click through & popups

(defn log-click-and-navigate [query trigger & [ignore-params?]]
  (let [payload (->> {:query   query
                      :trigger trigger}
                     (clj->js)
                     (.stringify js/JSON))]
    (.sendBeacon
      js/navigator
      (str js/apiURL "/api/log-click-through")
      (js/Blob.
        [payload]
        (clj->js {:type "application/json"}))))
  (rf/dispatch [:clear-trigger])
  (let [params      (-> js/window .-location .-href (url/url) :query (dissoc "query"))
        destination (if ignore-params?
                      (:destination trigger)
                      (-> (url/url (:destination trigger))
                          (assoc :query params)
                          str))]
    (set! (. js/location -href) destination)))

(rf/reg-sub
  :selected-trigger
  (fn [db _]
    (:selected-trigger db)))

(rf/reg-event-db
  :clear-trigger
  (fn [db _]
    (dissoc db :selected-trigger)))

(rf/reg-event-fx
  :confirm-trigger
  (fn [{:keys [db]} _]
    (let [query   (str (get-in db [:search :query]))
          trigger (:selected-trigger db)]
      {:side-effect #(log-click-and-navigate query trigger)})))

(rf/reg-event-fx
  :select-trigger
  (fn [{:keys [db]} [_ trigger {:keys [ignore-params?]}]]
    (let [query (str (get-in db [:search :query]))]
      (if (not-empty (:message trigger))
        {:db (assoc db :selected-trigger trigger)}
        {:side-effect #(log-click-and-navigate query trigger ignore-params?)}))))

;; -------------------------
(rf/reg-event-db
  :search/cycle-prompt
  (fn [db _]
    (let [num-prompts    (count (get-in db [:search :prompts]))
          current-prompt (get-in db [:search :selected-prompt])]
      (update-in db [:search :selected-prompt] #(mod (inc current-prompt) num-prompts)))))

(rf/reg-sub
  :search/prompt
  (fn [db _]
    (get (get-in db [:search :prompts])
         (get-in db [:search :selected-prompt]))))

(rf/reg-sub
  :search/placeholder
  (fn [db _]
    (get (get-in db [:search :placeholders])
         (get-in db [:search :selected-prompt]))))

(rf/reg-event-db
  :search/set-prompt-class
  (fn [db [_ class]]
    (assoc-in db [:search :prompt-class] class)))

(rf/reg-sub
  :search/prompt-class
  (fn [db _]
    (get-in db [:search :prompt-class])))
