(ns story-discovery-frontend.effects
  (:require
    [re-frame.core :as rf]))

(defn- dispatch-or-fn [f resource-id]
  (cond
    (nil? f) (rf/dispatch [:http/set-loaded resource-id])
    (fn? f) #(do (rf/dispatch [:http/set-loaded resource-id])
                 (f %))
    (sequential? f) #(do (rf/dispatch [:http/set-loaded resource-id])
                         (rf/dispatch (conj f %)))))

(rf/reg-fx
  :http
  (fn [{:keys [method url resource-id params on-success on-error]}]
    (rf/dispatch [:http/set-loading resource-id])
    (let [on-error (or on-error [:http/ajax-error resource-id])]
      (js/setTimeout
        #(method (str js/apiURL url)
                 {:handler       (dispatch-or-fn on-success resource-id)
                  :params        params
                  :error-handler (dispatch-or-fn on-error resource-id)})
        500))))

(rf/reg-sub
  :http/loading?
  (fn [db _]
    (boolean (not-empty (:http/pending-resources db)))))

(rf/reg-sub
  :http/loading-triggers?
  (fn [db _]
    (boolean (some (set (:http/pending-resources db)) #{:http/search-triggers :http/get-default-triggers}))))

(rf/reg-event-db
  :http/set-loading
  (fn [db [_ resource-id]]
    (-> db
        (update :http/pending-resources (fnil conj #{}) resource-id)
        (dissoc :http/error))))

(rf/reg-event-db
  :http/set-loaded
  (fn [db [_ resource-id]]
    (update db :http/pending-resources disj resource-id)))

(rf/reg-sub
  :http/error
  (fn [db _]
    (:http/error db)))

(rf/reg-event-db
  :http/ajax-error
  (fn [db [_ resource-id]]
    (assoc db :http/error resource-id)))

(rf/reg-cofx
  :now
  (fn [cofx _]
    (assoc cofx :now (.getTime (js/Date.)))))

(rf/reg-fx
  :side-effect
  (fn [f]
    (f)))