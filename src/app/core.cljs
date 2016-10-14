(ns app.core
  (:require [reagent.core :as r]
            [cljsjs.vis]
            [goog.object :as gobj]))

(enable-console-print!)

(def state (r/atom {:registers               {0 {:label "R0" :value 0}
                                              1 {:label "R1" :value 4}
                                              2 {:label "R2" :value 3}
                                              3 {:label "R3" :value 0}}
                    :nodes                   {0 {:type :start, :x 0 :y 0}
                                              1 {:register 1 :type :dec :x 0 :y 100}
                                              2 {:register 2 :type :dec}
                                              3 {:register 0 :type :inc}
                                              4 {:register 3 :type :inc}
                                              5 {:register 3 :type :dec}
                                              6 {:register 2 :type :inc}
                                              7 {:type :end}}
                    :edges                   {0 {:type :right :start 0 :end 1 :color "green"}
                                              1 {:type :left :start 1 :end 7 :color "red"}
                                              2 {:type :right :start 1 :end 2 :color "green"}
                                              3 {:type :left :start 2 :end 5 :color "red"}
                                              4 {:type :right :start 2 :end 3 :color "green"}
                                              5 {:type :right :start 3 :end 4 :color "green"}
                                              6 {:type :right :start 4 :end 2 :color "green"}
                                              7 {:type :left :start 5 :end 1 :color "red"}
                                              8 {:type :right :start 5 :end 6 :color "green"}
                                              9 {:type :right :start 6 :end 5 :color "green"}}
                    :modals                  {:add-register {:open  false
                                                             :title "Add Register"
                                                             :data  {:register-label ""}}
                                              :add-node     {:open  false
                                                             :title "Add Node"}
                                              :add-edge     {:open  false
                                                             :title "Add Edge"}}
                    :current-node-type       :dec
                    :current-connection-type :all
                    :current-node            0}))

(defn empty-machine []
  {:registers {}
   :nodes     {0 {:type :start}
               1 {:type :end}}
   :edges     {}})

(defn get-path [node-id state]
  (let [edges (filter #(= node-id (:start (val %))) (:edges state))
        node (get-in state [:nodes node-id])
        registers (:registers state)]
    (cond (= 1 (count edges)) (first edges)
          (zero? (:value (registers (:register node)))) (first (filter #(= :left (:type (val %))) edges))
          :else (first (filter #(= :right (:type (val %))) edges)))))

(defn get-node-label [id]
  (cond (= (get-in @state [:nodes id :type]) :start) "Start"
        (= (get-in @state [:nodes id :type]) :end) "End"
        :else (let [register-id (get-in @state [:nodes id :register])]
                (str (get-in @state [:registers register-id :label]) (cond (= (get-in @state [:nodes id :type]) :inc) "+"
                                                                           (= (get-in @state [:nodes id :type]) :dec) "-")
                     ;;" (" (get-in @state [:registers register-id :value]) ")"
                     ))))

(defn cljs->visjs [s]
  (let [state @s
        nodes (map (fn [[id {:keys [type register]}]]
                     (case type
                       :start {:id id :label "Start" :shape "box"}
                       :end {:id id :label "Halt" :shape "box"}
                       {:id    id
                        :label (get-node-label id)
                        :value (get-in state [:registers register :value])})) (:nodes state))
        edges (map (fn [[_ {:keys [start end color]}]]
                     {:from start :to end :color color}) (:edges state))]
    (clj->js {:nodes nodes :edges edges})))

(defn update-register [register node]
  (cond (= :inc (:type node)) (update register :value inc)
        (= :dec (:type node)) (update register :value dec)
        :else (assoc register :label "Test")))

(defn update-node-labels [network register]
  )

(defn take-step [s network]
  (let [state @s
        node-id (:current-node state)
        node (get-in state [:nodes node-id])
        register (get-in state [:registers (:register node)])]
    (if (and (or (not (zero? (:value register)))
                 (not= (:type node) :dec))
             (contains? node :register))
      (do (swap! s assoc-in [:registers (:register node)] (update-register register node))
          (update-node-labels network register)))
    (swap! s assoc :current-node (:end (val (get-path node-id state))))
    (.selectNodes network #js [(:current-node @s)])))


(defn modal [id s content on-okay]
  (let [data (get-in s [:modals id])]
    (if (:open data)
      [:div.modal
       [:div.overlay]
       [:div.panel
        [:header.title [:h2 (:title data)]]
        [:div.content content]
        [:div.buttons
         [:button {:on-click on-okay} "Ok"]
         [:button {:on-click (fn [e] (swap! state assoc-in [:modals id :open] false))} "Cancel"]]]]
      nil)))

(defn add-register-modal [s]
  (modal :add-register
         s
         [:div
          [:div
           [:label {:for "new-register-label"} "Label: "]
           [:input#new-register-label {:type      "text" :value (get-in s [:modals :add-register :data :register-label])
                                       :on-change (fn [e]
                                                    (swap! state assoc-in [:modals :add-register :data :register-label] (.-value (.-target e))))}]]
          [:div
           [:label {:for "new-register-value"} "Value: "]
           [:input#new-register-value {:type      "text" :value (get-in s [:modals :add-register :data :register-value])
                                       :on-change (fn [e]
                                                    (swap! state assoc-in [:modals :add-register :data :register-value] (.-value (.-target e))))}]]]
         (fn [e]
           (swap! state assoc-in [:registers (inc (reduce max -1 (-> s :registers keys)))]
                  {:label (get-in s [:modals :add-register :data :register-label])
                   :value (js/parseInt (get-in s [:modals :add-register :data :register-value]))})
           (swap! state assoc-in [:modals :add-register :open] false))))


(defn draw-registers []
  (fn []
    [:div
     [:h2 "Registers:"]
     [:div.registers (map (fn [[id v]]
                            [:div {:on-click (fn [e] (swap! state assoc :current-register id))
                                   :class    (if (= id (:current-register @state)) "register selected" "register")} (:label v) ": " (:value v)]) (:registers @state))
      [:div.add-register {:on-click (fn [e] (swap! state assoc-in [:modals :add-register :open] true))} "Add"]]]))

(defn controls [network]
  (fn []
    [:div.controls
     [:div.button {:on-click (fn [e] (.addNodeMode network))} "Add Node"]
     [:div.button {:on-click (fn [e] (.addEdgeMode network))} "Add Edge"]
     [:div.button {:on-click (fn [e] (swap! state assoc :registers (:registers (empty-machine))
                                            :nodes (:nodes (empty-machine))
                                            :edges (:edges (empty-machine))
                                            :current-node 0)
                               (.setData network (cljs->visjs state))
                               (.selectNodes network #js [(:current-node @state)]))} "Clear"]

     [:div.edge-types
      [:div.label "Branch type:"]
      [:div {:class    (if (= :left (:current-connection-type @state)) "edge-type button selected" "button edge-type")
             :on-click (fn [e] (swap! state assoc :current-connection-type :left))} "Less than 0"]
      [:div {:class    (if (= :right (:current-connection-type @state)) "edge-type button selected" "button edge-type")
             :on-click (fn [e] (swap! state assoc :current-connection-type :right))} "Greater than 0"]]

     [:div.node-types
      [:div.label "Node type:"]
      [:div {:class    (if (= :inc (:current-node-type @state)) "node-type button selected" "node-type button")
             :on-click (fn [e] (swap! state assoc :current-node-type :inc))} "+"]
      [:div {:class    (if (= :dec (:current-node-type @state)) "node-type button selected" "node-type button")
             :on-click (fn [e] (swap! state assoc :current-node-type :dec))} "-"]]]))

(defn machine-controls [network]
  (fn []
    [:div
     [:div.controls
      [:div.button {:on-click (fn [e] (take-step state network))} "Take Step"]]
     [:div.modals (add-register-modal @state)]]))

(defn main []
  (r/render-component [draw-registers] (.getElementById js/document "registers"))
  (let [network (js/vis.Network. (.getElementById js/document "graph-container")
                                 (cljs->visjs state) (clj->js {:nodes        {:fixed   false
                                                                              :scaling {:min 10 :max 10}
                                                                              :physics false
                                                                              :font { :size 20 }}
                                                               :edges        {:arrows         {:to true}
                                                                              :smooth         {:type "horizontal"}
                                                                              :selectionWidth 0}
                                                               :layout       {
                                                                              ;:randomSeed 300
                                                                              :improvedLayout true
                                                                              }
                                                               :physics      {:enabled false}
                                                               :manipulation {:enabled         true
                                                                              :initiallyActive true
                                                                              :addNode         (fn [data callback]
                                                                                                 (let [id (inc (reduce max -1 (-> @state :nodes keys)))
                                                                                                       s (swap! state assoc-in [:nodes id]
                                                                                                                {:register (get @state :current-register)
                                                                                                                 :type     (get @state :current-node-type)})]
                                                                                                   (gobj/set data "id" id)
                                                                                                   (gobj/set data "label" (get-node-label id))
                                                                                                   (callback data)))
                                                                              :addEdge         (fn [data callback]
                                                                                                 (let [id (inc (reduce max -1 (-> @state :edges keys)))
                                                                                                       s (swap! state assoc-in [:edges id]
                                                                                                                {:type  (get @state :current-connection-type)
                                                                                                                 :start (gobj/get data "from")
                                                                                                                 :end   (gobj/get data "to")
                                                                                                                 :color (if (= :left (get @state :current-connection-type)) "red" "green")})]
                                                                                                   (gobj/set data "color" (if (= :left (get @state :current-connection-type)) "red" "green"))
                                                                                                   (callback data)
                                                                                                   )
                                                                                                 )}
                                                               }))]
    (.selectNodes network #js [(:current-node @state)])
    (gobj/set js/window "network" network)
    (r/render-component [(controls network)] (.getElementById js/document "graph-controls"))
    (r/render-component [(machine-controls network)] (.getElementById js/document "machine-controls"))))
