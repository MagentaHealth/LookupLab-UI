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

(ns story-discovery-frontend.core
  (:require
    [cemerick.url :as url]
    [clojure.string :as s]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.dom :as d]
    ;;
    [story-discovery-frontend.effects]
    [story-discovery-frontend.events]))


;; -------------------------
;; Helpers

(defn sentence-case [s]
  (apply str (s/upper-case (first s)) (rest s)))

(defn trigger-count [triggers]
  (str " (" (count triggers) " result" (if (> (count triggers) 1) "s") ")"))

;; -------------------------
;; Components

(defn trigger-loader []
  [:<>
   (for [[idx ver] (map-indexed vector [0 1 0 2 1 0 2 2 1])]
     ^{:key (str idx ver)}
     [:div.block {:class (str "trigger-loader-" ver)}])])

(defn trigger-card [trigger & [{show-icon? :show-icon?}]]
  [:div.trigger-card.py-2.is-flex
   {:on-click #(rf/dispatch [:select-trigger trigger])}
   [:p.is-flex-grow-1
    [:span (str (:prefix trigger) "...")]
    [:br]
    [:span.trigger-description.primary-text.has-text-weight-bold
     (sentence-case (:description trigger))]]
   [:span.icon.my-auto.is-hidden-tablet.has-text-grey-lighter
    [:i.fa.fa-chevron-right]]])

(defn trigger-list [triggers showing-defaults?]
  (r/with-let [expanded?   (r/atom false)
               max-results (if showing-defaults? 10000 5)]
    [:<>
     (for [trigger (if @expanded? triggers (take max-results triggers))]
       ^{:key (str (:audience trigger) (:description trigger))}
       [trigger-card trigger])

     (let [remaining-triggers (- (count triggers) max-results)]
       (when (and (not @expanded?)
                  (pos? remaining-triggers))
         [:div.trigger-card.py-2.is-flex.is-justify-content-center
          [:a.has-text-weight-normal.has-text-grey-dark
           {:on-click #(reset! expanded? true)}
           [:i.fa.fa-plus.primary-text]
           (str " show " remaining-triggers " more result" (if (> remaining-triggers 1) "s"))]]))]))

(defn trigger-group [triggers audience showing-defaults?]
  (when (contains? @(rf/subscribe [:search/audience]) audience)
    (when-let [triggers (not-empty (get triggers audience))]
      [:div
       (when-not (and showing-defaults? (= "patients" js/audienceConfig))
         [:div.content.sticky-audience-heading.mb-0
          [:h5.audience-heading.py-2.pl-2.mb-2
           (str
             (when (not (= "patients" js/audienceConfig)) audience)
             (when-not showing-defaults? (trigger-count triggers)))]])
       [trigger-list triggers showing-defaults?]])))

(defn trigger-panel [triggers & [{:keys [showing-defaults?]}]]
  (let [triggers (if (= "patients" js/audienceConfig)
                   (-> triggers
                       (update "Current Patients" #(concat % (get triggers "All")))
                       (dissoc "All"))
                   triggers)]
    [:<>
     (for [audience ["Current Patients" "Non Patients" "Third Parties" "All"]]
       ^{:key (str audience "-group")}
       [trigger-group triggers audience showing-defaults?])]))

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
   [:p.mb-2 "Please first"]
   [:a.button.is-primary.is-fullwidth
    {:on-click #(.reload js/location)}
    [:span
     "Reload and Try Again"]
    [:span.icon
     [:i.fa.fa-rotate-right]]]
   [:p.mb-2.mt-4
    "If this error continues"]
   [:a.button.is-primary.is-fullwidth.is-outlined
    {:on-click #(rf/dispatch [:select-trigger {:destination js/homePage} {:ignore-params? true}])}
    [:span
     "Proceed to our Main Website"]
    [:span.icon
     [:i.fa.fa-arrow-right]]]])

(defn no-results []
  [:div.content
   [:p.title.has-text-weight-normal.mb-5 "No Results :("]

   (if (= "patients" js/audienceConfig)
     [:<>
      [:p [:strong "Please try again using more general keywords"] " - like " [:strong "\"book a well baby\""] " or " [:strong "\"get my medical records\"."]]
      [:p "How might you answer a receptionist who asks you the question - how can I help you today?"]
      [:hr.mt-4.mb-3]

      [:p "OR, if this isn't a good time, just skip ahead and " [:strong "go to your physician's old booking page:"]]
      [:div.buttons.is-centered
       [:a.button.is-secondary
        {:on-click #(rf/dispatch [:select-trigger {:destination js/bookingPage}])}
        [:span
         "Take me to the old booking page"]]]
      [:p "If you need help with an " [:strong "administrative question"] ", "
       [:a {:on-click #(rf/dispatch [:select-trigger {:destination js/resourcesPage} {:ignore-params? true}])} "click here to see our FAQ and Contact Information"] "."]]

     [:<>
      [:p [:strong "Please try again using more general keywords,"] " or click the option below that describes you best"]
      (when (= "all" js/audienceConfig)
        [:<>
         [:h5.audience-heading.has-background-grey-dark.py-2.pl-2.mt-5.mb-2 "Current Patients"]
         [:p "If you need help with an " [:strong "administrative question"] ", "
          [:a {:on-click #(rf/dispatch [:select-trigger {:destination js/resourcesPage} {:ignore-params? true}])} "click here to see our FAQ and Contact Information"] "."]])

      [:h5.audience-heading.has-background-grey-dark.py-2.pl-2.mt-5.mb-2 "Non Patients"]
      [:p
       [:a {:on-click #(rf/dispatch [:select-trigger {:destination js/registrationPage} {:ignore-params? true}])}
        (str "More information about registration / seeing a physician at " js/orgName " is available here")]
       "."]

      [:h5.audience-heading.has-background-grey-dark.py-2.pl-2.mt-5.mb-2 "Third Parties"]
      [:p
       [:a {:on-click #(rf/dispatch [:select-trigger {:destination js/thirdPartyPage} {:ignore-params? true}])}
        "Our contact information for third parties such as pharmacies and insurance companies is available here"] "."]
      [:hr.mt-4.mb-3]
      [:p.mb-2 "Lastly, you can skip this tool and"]
      [:a.button.is-primary.is-fullwidth
       {:on-click #(rf/dispatch [:select-trigger {:destination js/homePage} {:ignore-params? true}])}
       [:span
        "Proceed to our Main Website"]
       [:span.icon
        [:i.fa.fa-arrow-right]]]])])

(defn popup-message []
  (if-let [trigger (not-empty @(rf/subscribe [:selected-trigger]))]
    [:div.modal.is-active
     {:style {:z-index 9999}}
     [:div.modal-background]
     [:div.modal-content
      [:article.message
       [:div.message-header
        [:p (apply str (s/upper-case (first (:description trigger))) (rest (:description trigger)))]
        [:button.delete
         {:aria-label "close"
          :on-click   #(rf/dispatch [:clear-trigger])}]]
       [:div.message-body
        [:p.mb-4
         {:dangerouslySetInnerHTML
          {:__html (:message trigger)}}]
        [:a.button.is-primary
         {:on-click #(rf/dispatch [:confirm-trigger])}
         [:span "Continue"]
         [:span.icon
          [:i.fa.fa-arrow-right]]]]]]]))

(defn search-view []
  [:<>
   [popup-message]
   [:div.columns.h-100
    [:div.column.is-7.m-auto.search-column
     [:div.content
      [:p.subtitle "We're testing a new search tool!"]
      [:h2.title.is-size-4 "How can we help you today?"]]

     [:div.field.mb-1
      {:class @(rf/subscribe [:search/prompt-class])}
      [:label.label.mb-1.mt-3
       {:style {:cursor "default"}}
       @(rf/subscribe [:search/prompt])]
      [:div.control
       [:input.input
        {:type        "search"
         :placeholder @(rf/subscribe [:search/placeholder])
         :value       @(rf/subscribe [:search/query])
         :on-change   #(rf/dispatch [:set-query (-> % .-target .-value)])
         :on-key-up   #(if (= 13 (.-which %))
                         (.blur (.-target %)))}]]]

     (case js/audienceConfig
       "others"
       [:div.field.mb-2
        [audience-selector
         [["Non patients" "Non Patients"]
          ["Third parties" "Third Parties"]]]]

       "all"
       [:div.field.mb-2
        [audience-selector
         [["Current patients" "Current Patients"]
          ["Non patients" "Non Patients"]
          ["Third parties" "Third Parties"]]]]

       nil)

     [:a.has-text-grey-light.opt-out-link.is-size-7
      {:on-click #(rf/dispatch [:select-trigger {:destination js/homePage} {:ignore-params? true}])}
      "Click here if you would prefer to browse the website "
      [:span.icon-text {:style {:vertical-align :initial}}
       "manually"
       [:span.icon
        [:i.fa.fa-arrow-right]]]]]


    [:div.column.results-column.h-100.p-0.pr-3.my-3.ml-3
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
          [trigger-panel results]
          (if (= "patients" js/audienceConfig)
            [:div.content.mb-0
             [:hr.mt-4.mb-3]
             [:p.mb-2 "If these results aren't helpful, try using more general terms, or " [:strong "go to your physician's old booking page:"]]
             [:div.buttons.is-centered
              [:a.button.is-secondary
               {:on-click #(rf/dispatch [:select-trigger {:destination js/bookingPage}])}
               [:span
                "Take me to the old booking page"]]]
             [:p "If you need help with an " [:strong "administrative question"] ", "
              [:a {:on-click #(rf/dispatch [:select-trigger {:destination js/resourcesPage}])} "click here to see our FAQ and Contact Information"] "."]]
            [:div.content.mb-0
             [:hr.mt-4.mb-3]
             [:p.mb-2 "If these results aren't helpful, try using more general terms, or"]
             [:a
              {:on-click #(rf/dispatch [:select-trigger {:destination js/homePage} {:ignore-params? true}])}
              [:span
               "Proceed to our Main Website"]
              [:span.icon
               [:i.fa.fa-arrow-right]]]])]

         defaults
         [trigger-panel defaults {:showing-defaults? true}]))]]])


(defn animate-prompt []
  (rf/dispatch [:search/set-prompt-class "fade-out"])
  (js/setTimeout
    #(do (rf/dispatch [:search/set-prompt-class "fade-in"])
         (js/setTimeout (rf/dispatch [:search/cycle-prompt]) 500))
    500))

(defn page []
  (if-let [query (-> js/window .-location .-href (url/url) :query (get "query"))]
    (rf/dispatch [:set-query query])
    (rf/dispatch [:http/get-default-triggers]))
  (js/setInterval animate-prompt 7000)
  [search-view])

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [page] (.getElementById js/document "app")))

(defn ^:export init! []
  (rf/dispatch [:init-db])
  (js/setTimeout mount-root 1000))
