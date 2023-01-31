(ns story-discovery-frontend.core
  (:require
    [clojure.string :as s]
    [re-frame.core :as rf]
    [reagent.dom :as d]
    ;;
    [story-discovery-frontend.effects]
    [story-discovery-frontend.events]))


;; -------------------------
;; Components

(defn trigger-loader []
  [:<>
   [:div.block.trigger-loader]
   [:div.block.trigger-loader-1]
   [:div.block.trigger-loader]
   [:div.block.trigger-loader-2]
   [:div.block.trigger-loader-1]
   [:div.block.trigger-loader]
   [:div.block.trigger-loader-2]])

(defn trigger-card [trigger & [{show-icon? :show-icon?}]]
  [:div.trigger-card.py-2.is-flex
   {:on-click #(rf/dispatch [:select-trigger trigger])}
   [:p.is-flex-grow-1
    [:span (:prefix trigger) "..."]
    [:br]
    [:span.trigger-description.magenta-text.has-text-weight-bold
     (s/upper-case (first (:description trigger))) (rest (:description trigger))]]
   [:span.icon.my-auto.is-hidden-tablet.has-text-grey-lighter
    [:i.fa.fa-chevron-right]]])

(defn trigger-list [triggers]
  [:<>
   (for [trigger triggers]
     ^{:key (str (:audience trigger) (:description trigger))}
     [trigger-card trigger])])

(defn trigger-group [triggers audience show-counts?]
  (when (or (= "All" audience)
            (contains? @(rf/subscribe [:search/audience]) audience))
    (when-let [triggers (not-empty (get triggers audience))]
      [:<>
       [:div.content.mb-0
        [:h5.mt-5.mb-2
         audience
         (when show-counts?
           (str " (" (count triggers) " result" (if (> (count triggers) 1) "s") ")"))]
        [:hr.mt-0.mb-0]]
       [trigger-list triggers]])))

(defn trigger-panel [triggers show-counts?]
  [:<>
   [trigger-group triggers "Current Patients" show-counts?]
   [trigger-group triggers "Non Patients" show-counts?]
   [trigger-group triggers "Third Parties" show-counts?]
   [trigger-group triggers "All" show-counts?]])


;; ------

(defn audience-checkbox [label value]
  (let [checked?  (contains? @(rf/subscribe [:search/audience]) value)
        disabled? (or (and (not-empty @(rf/subscribe [:search/query]))
                           (not (nil? @(rf/subscribe [:search/results])))
                           (not (contains? @(rf/subscribe [:search/results]) value)))
                      (and
                        (not-empty @(rf/subscribe [:search/query]))
                        @(rf/subscribe [:search/no-results?])))]
    [:label.radio.is-size-6
     {:class (if disabled? "has-text-grey-light")}
     [:input {:type      "checkbox"
              :name      "audience"
              :checked   checked?
              :disabled  disabled?
              :on-change #(if checked?
                            (rf/dispatch [:unset-audience value])
                            (rf/dispatch [:set-audience value]))}]
     label]))

(defn audience-selector [options]
  [:<>
   (for [[label audience] options]
     ^{:key label}
     [audience-checkbox label audience])])

;; -------------------------
;; Views

(defn unexpected-error []
  [:div.content
   [:p.subtitle "Sorry, an unexpected error occurred."]
   [:p.mb-1 "Please first"]
   [:a.button.is-primary.is-fullwidth
    {:on-click #(.reload js/location)}
    [:span
     "Reload and Try Again"]
    [:span.icon
     [:i.fa.fa-rotate-right]]]
   [:p.mb-1.mt-4
    "If this error continues"]
   [:a.button.is-primary.is-fullwidth.is-outlined
    {:on-click #(rf/dispatch [:select-trigger {:destination "/home"}])}
    [:span
     "Proceed to our Main Website"]
    [:span.icon
     [:i.fa.fa-arrow-right]]]])

(defn no-results []
  [:div.content
   [:p.subtitle "No Results :("]
   [:p [:strong "Please try using only keywords,"] " or click the option below that describes you best"]

   [:h5.mt-5.mb-2 "Current Patients"]
   [:hr.mt-0.mb-0]
   [:p "If you have a " [:strong "medical concern"] ", "
    [:a {:on-click #(rf/dispatch [:select-trigger {:destination "/medical-concern-directions"}])} "click here for guidance"] "."]
   [:p "If you need help with an " [:strong "administrative question"] ", "
    [:a {:on-click #(rf/dispatch [:select-trigger {:destination "/resources"}])} "click here to see our FAQ and Contact Information"] "."]

   [:h5.mt-5.mb-2 "Non Patients"]
   [:hr.mt-0.mb-0]
   [:p
    [:a {:on-click #(rf/dispatch [:select-trigger {:destination "/patientpreregistration"}])}
     "More information about registration / seeing a physician at Magenta Health is available here"]
     "."]

   [:h5.mt-5.mb-2 "Third Parties"]
   [:hr.mt-0.mb-0]
   [:p
    [:a {:on-click #(rf/dispatch [:select-trigger {:destination "/information-for-physicians-and-healthcare-facilities"}])}
     "Our contact information for third parties such as pharmacies and insurance companies is available here"] "."]

   [:hr.mb-2]
   [:p.mb-1 "Lastly, you can skip this tool and"]
   [:a.button.is-primary.is-fullwidth
    {:on-click #(rf/dispatch [:select-trigger {:destination "/home"}])}
    [:span
     "Proceed to our Main Website"]
    [:span.icon
     [:i.fa.fa-arrow-right]]]])

(defn popup-message []
  (if-let [message (not-empty @(rf/subscribe [:trigger/message]))]
    [:div.modal.is-active
     {:style {:z-index 9999}}
     [:div.modal-background]
     [:div.modal-content
      [:div.box
       [:div.content.has-text-centered
        [:p {:dangerouslySetInnerHTML
             {:__html message}}]
        [:p.redirecting [:i "Redirecting you momentarily"]]]]]]))

(defn search-view []
  [:<>
   [popup-message]
   [:div.columns.h-100
    [:div.column.is-7.m-auto
     [:div.content
      [:p.subtitle "We're testing a new search tool."]
      [:h2.title.is-size-4 "How can we help you today?"]]

     [:div.field.mb-1
      [:label.label.mb-1.mt-3 "I want..."]
      [:div.control
       [:input.input
        {:type        "search"
         :placeholder "to schedule an appointment"
         :value       @(rf/subscribe [:search/query])
         :on-change   #(rf/dispatch [:set-query (-> % .-target .-value)])
         :on-key-up   #(if (= 13 (.-which %))
                         (.blur (.-target %)))}]]]

     [:div.field.mb-2
      [audience-selector
       [["Current patients" "Current Patients"]
        ["Non patients" "Non Patients"]
        ["Third parties" "Third Parties"]]]]

     [:a.has-text-grey-light.opt-out-link.is-size-7
      {:on-click #(rf/dispatch [:select-trigger {:destination "/home"}])}
      "Click here if you would prefer to browse the website "
      [:span.icon-text {:style {:vertical-align :initial}}
       "manually"
       [:span.icon
        [:i.fa.fa-arrow-right]]]]]


    [:div.column.h-100.overflow-scroll
     (let [results  (not-empty @(rf/subscribe [:search/results]))
           defaults (not-empty @(rf/subscribe [:triggers/default]))]
       (cond
         @(rf/subscribe [:http/loading-triggers?])
         [trigger-loader]

         @(rf/subscribe [:http/error])
         [unexpected-error]

         (and (not-empty @(rf/subscribe [:search/query]))
              @(rf/subscribe [:search/no-results?]))
         [no-results]

         results
         [:<>
          [trigger-panel results true]
          [:div.content.mb-0
           [:hr.mt-4.mb-3]
           [:p.mb-2 "If these results aren't helpful, just"]
           [:a.button.is-primary.is-fullwidth
            {:on-click #(rf/dispatch [:select-trigger {:destination "/home"}])}
            [:span
             "Proceed to our Main Website"]
            [:span.icon
             [:i.fa.fa-arrow-right]]]]]

         defaults
         [trigger-panel defaults]))

     #_[:button.button.is-primary.is-pulled-right
        {:on-click #(rf/dispatch [:set-view :browse])}
        [:span.icon-text
         [:span "Browse more"]
         [:span.icon
          [:i.fa.fa-arrow-right]]]]]]])


(defn browse-view []
  (when-let [triggers @(rf/subscribe [:triggers/all])]
    [:<>
     [:div.is-flex
      [:button.button
       {:on-click #(rf/dispatch [:set-view :search])}
       [:i.fa-solid.fa-x]]
      [:div.content
       [:h2.subtitle "How can we help you today?"]]]
     [:div.columns.h-100.overflow-scroll                    ;; need h-100 - header height
      [:div.column
       [trigger-group triggers "Current Patients"]]
      [:div.column
       [trigger-group triggers "Non Patients"]]
      [:div.column
       [trigger-group triggers "Third Parties"]]]]))

(defn page []
  (condp = @(rf/subscribe [:view])
    :search (do (rf/dispatch [:http/get-default-triggers])
                [search-view])
    :browse (do (rf/dispatch [:http/get-all-triggers])
                [browse-view])
    (do (rf/dispatch [:http/get-default-triggers])
        [search-view])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [page] (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch [:init-db])
  (js/setTimeout mount-root 300))
