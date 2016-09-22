(ns app.core
  (:require [app.tetrominoes :refer [tetrominoes rotate-tetromino]]
            [app.control :refer [bind-key-handlers move-down]]
            [cljs.core.async :refer [chan close! <!]]
            [app.component :refer [render]]
            [reagent.core :as r])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]))

(enable-console-print!)

(defonce game-state (r/atom
                      {:state       :not-started
                       :game-speed  1000
                       :grid-height 22
                       :grid-width  10
                       :score       0
                       :level       0}))

(defn create-empty-grid
  [width height]
  (vec (take height (for [rows (range)]
                      (vec (for [cols (range width)]
                             []))))))

(defn game-over?
  [state]
  (let [empty-rows (filter (fn [row]
                             (every? empty? row)) (:grid state))]
    (empty? empty-rows)))

(defn drop-rows
  [state]
  (let [not-emp (filter (fn [row]
                          (not (every? empty? row))) (:grid state))]
    (assoc state :grid (vec (reverse (take (:grid-height state)
                                           (concat (reverse not-emp)
                                                   (create-empty-grid (:grid-width state)
                                                                      (:grid-height state)))))))))

(defn remove-completed-lines
  [state]
  (assoc state :grid (mapv (fn [row]
                             (if (every? true? (map (comp not empty?) row))
                               (vec (for [cols (range (:grid-width state))]
                                      []))
                               row)) (:grid state))))

(defn completed-lines
  [state]
  (filter (fn [row]
            (every? (comp not empty?) row)) (:grid state)))

(defn update-score []
  (let [state @game-state
        n (count (completed-lines state))
        l (:level state)
        score-to-add (case n
                       1 (* 40 (inc l))
                       2 (* 100 (inc l))
                       3 (* 300 (inc l))
                       4 (* 1200 (inc l))
                       0 0)]
    (swap! game-state update :score (partial + score-to-add))))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn start-game-loop
  []
  (go
    (while true
      (<! (timeout (:game-speed @game-state)))
      (if (= (:state @game-state) :running)
        (do (swap! game-state move-down)
            (update-score)
            (swap! game-state remove-completed-lines)
            (swap! game-state drop-rows)
            (if (game-over? @game-state)
              (swap! game-state assoc :state :game-over)))))))

(defn init-game []
  (let [[current a b c & r] (tetrominoes)
        gw (:grid-width @game-state)
        gh (:grid-height @game-state)]
    (do
      (start-game-loop)
      (swap! game-state assoc
             :state :running
             :current-tetromino current
             :next-tetrominoes [a b c]
             :grid (create-empty-grid gw gh))
      (bind-key-handlers game-state))))

(defn main []
  (init-game)
  (render game-state))

