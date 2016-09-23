(ns app.control
  (:require [app.tetrominoes :refer [rotate-tetromino tetrominoes]]
            [cljs.core.async :refer [chan close! <! >! put! pipeline]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(defn tetromino->coords
  [t]
  (filter (comp not nil?)
          (mapcat (fn [row y-coord]
                    (map (fn [col x-coord]
                           (if (= 1 col) [x-coord y-coord])) row (iterate inc (:x t))))
                  (:coordinates t) (iterate inc (:y t)))))

(defn can-move-to?
  [state new-coordinates]
  (every? true? (map (fn [[col row]]
                       (if-let [available (get-in state [:grid row col])]
                         (empty? available)
                         false))
                     new-coordinates)))

(defn can-move?
  [state axis transform]
  (let [tetromino (:current-tetromino state)
        new-coordinates (tetromino->coords (update tetromino axis transform))]
    (can-move-to? state new-coordinates)))

(defn can-move-down?
  [state]
  (can-move? state :y inc))

(defn can-rotate?
  [state]
  (can-move-to? state (-> state :current-tetromino rotate-tetromino tetromino->coords)))

(defn move
  [state axis transform]
  (if (can-move? state axis transform)
    (update-in state [:current-tetromino axis] transform)
    state))

(defn rotate
  [state]
  (if (can-rotate? state)
    (update-in state [:current-tetromino] rotate-tetromino)
    state))

(defn merge-tetromino-into-grid [state]
  (let [type (:type (:current-tetromino state))
        coordinates (tetromino->coords (:current-tetromino state))]
    (reduce (fn [s [col row]]
              (assoc-in s [:grid row col] [type])) state coordinates)))

(defn move-down
  [state]
  (if (can-move-down? state) (update-in state [:current-tetromino :y] inc)
                             (-> state
                                 merge-tetromino-into-grid
                                 (assoc :current-tetromino (first (:next-tetrominoes state))
                                        :next-tetrominoes (concat (rest (:next-tetrominoes state)) (take 1 (tetrominoes)))))))
(defn toggle-pause
  [state]
  (case (:state state)
    :running (assoc state :state :paused)
    :paused (assoc state :state :running)))

(defn filter-keycodes
  [c]
  (let [out-chan (chan)]
    (pipeline 1 out-chan (filter #(some #{(.-keyCode %)} [32 37 38 39 40])) c)
    out-chan))

(defn event-handler
  [chan game-state]
  (let [events-chan (filter-keycodes chan)]
    (go
      (while true
        (let [evt (<! events-chan)
              code (.-keyCode evt)]
          (.preventDefault evt) 
          (cond
            (= code 37) (swap! game-state move :x dec)
            (= code 39) (swap! game-state move :x inc)
            (= code 38) (swap! game-state rotate)
            (= code 40) (swap! game-state move-down)
            (= code 32) (swap! game-state toggle-pause)))))))

(defn bind-key-handlers
  [c]
  (.addEventListener
    js/window "keydown" (fn [evt]
                          (go (>! c evt)))))