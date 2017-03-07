tmux new-session -d -s eti
tmux split-window -t eti:1 -v
tmux rename-window main
tmux send-keys -t eti:1.1 "vim src/cljs/eti/core.cljs" "Enter"
tmux send-keys -t eti:1.2 "lein repl" "Enter"
tmux new-window -t eti:2
tmux select-window -t eti:2
tmux rename-window server
tmux select-window -t eti:1
tmux attach -t eti
