;; Copyright (C) 2022-2023 Magenta Health Inc. 
;; Authored by Carmen La <https://carmen.la/>.

;; This file is part of LookupLab-UI.

;; LookupLab-UI is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as
;; published by the Free Software Foundation, either version 3 of the
;; License, or (at your option) any later version.

;; LookupLab-UI is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with LookupLab-UI.  If not, see <https://www.gnu.org/licenses/>.

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