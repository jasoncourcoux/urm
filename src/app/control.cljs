(ns app.control
  (:require [app.tetrominoes :refer [rotate-tetromino tetrominoes]]))

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
                       (cond (>= col (:grid-width state)) false
                             (< col 0) false
                             (>= row (count (:grid state))) false
                             (empty? (get-in state [:grid row col])) true
                             :else false)) new-coordinates)))

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
                                        :next-tetrominoes (conj (rest (:next-tetrominoes state)) (first (tetrominoes)))))))
(defn toggle-pause
  [state]
  (case (:state state)
    :running (assoc state :state :paused)
    :paused (assoc state :state :running)))

(defn bind-key-handlers
  [game-state]
  (.addEventListener js/window
                     "keydown"
                     (fn [e]
                       (let [code (.-keyCode e)
                             LEFT 37 RIGHT 39 UP 38 DOWN 40 SPACE 32
                             allowed-codes [LEFT RIGHT UP DOWN SPACE]]
                         (if (some #{code} allowed-codes)
                           (do (.preventDefault e)
                               (if (= :running (:state @game-state))
                                 (cond
                                   (= code LEFT) (swap! game-state move :x dec)
                                   (= code RIGHT) (swap! game-state move :x inc)
                                   (= code UP) (swap! game-state rotate)
                                   (= code DOWN) (swap! game-state move-down)
                                   (= code SPACE) (swap! game-state toggle-pause))
                                 (if (= code SPACE) (swap! game-state toggle-pause)))))))))
