(ns reactive-console.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reactive-console.handlers :as handlers]
            [reactive-console.subs :as subs]
            [reactive-console.editor :as editor]
            [reactive-console.common :as common]
            [reactive-console.utils :as utils]))

;;; many parts are taken from jaredly's reepl
;;; https://github.com/jaredly/reepl

(defn display-output-item
  ([console-key value]
   (display-output-item console-key value false))
  ([console-key value error?]
   [:div
    {:on-click #(dispatch [:focus-console-editor console-key])
     :class (str "cm-console-item" (when error? " cm-console-item-error"))}
    value]))

(defn display-repl-item
  [console-key item]
  (if-let [text (:text item)]
    [:div.cm-console-item
     {:on-click #(do (dispatch [:console-set-text console-key text])
                     (dispatch [:focus-console-editor console-key]))}
     [utils/colored-text (str (:ns item) "=> " text)]]

    (if (= :error (:type item))
      (display-output-item console-key (.-message (:value item)) true)
      (display-output-item console-key (:value item)))))

(defn repl-items [console-key items]
  (into [:div] (map (partial display-repl-item console-key) items)))

(defn console [console-key eval-opts]
  (let [items (subscribe [:get-console-items console-key])
        text  (subscribe [:get-console-current-text console-key])]
    (dispatch-sync [:init-console console-key eval-opts])
    (reagent/create-class
     {:reagent-render
      (fn []
        [:div.cm-console-container
         [:div.cm-console
          {:on-click #(dispatch [:focus-console-editor console-key])}
          [repl-items console-key @items]
          [editor/editor console-key text]]])
      :component-did-update
      (fn [this]
        (common/scroll-to-el-bottom! (reagent/dom-node this)))})))
