#!/bin/bash -x

if [[ "$2" eq "fleaky" ]]; then
  echo "========= FLEAKY $2 node... ========="
elif [[ "$2" eq "heal" ]]; then
  echo "========= HEAL $2 node... ========="
fi


nodeF_1=18.194.58.196
nodeF_2=54.93.164.15
nodeF_3=54.93.164.15
nodeI_1=54.171.122.189
nodeI_2=54.171.122.189
nodeI_3=34.253.187.2

all=( "$nodeF_1" "$nodeF_2" "$nodeF_3" "$nodeI_1" "$nodeI_2" "$nodeI_3" )

for node in "${all[@]}"; do
  echo "============ Staging MultiDC App on:       $node     ... ============"

  read -r -d '' COMMAND <<'EOF'
    tc qdisc change dev ens3 root netem reorder 0.02 duplicate 0.05 corrupt 0.01 delay 1
EOF

  ssh -i $HOME/.ssh/replicated-entity.pem ubuntu@${node} $COMMAND
  ssh -i $HOME/.ssh/re-central.pem ubuntu@${node} $COMMAND

done

