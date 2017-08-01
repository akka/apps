for node_number in 002 003 004 005 006 007 008 009 010
do
  echo "Creating new node akka-sharding-${node_number}..."
  gcloud compute --project "akka-gcp" disks create "akka-ddata-${node_number}" --size "10" --zone "us-central1-a" --source-snapshot "ddata-node-snapshot" --type "pd-standard"

  gcloud compute --project "akka-gcp" instances create "akka-ddata-${node_number}" --zone "us-central1-a" \
  --machine-type "n1-standard-4" --subnet "default" \
  --no-address\
  --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring.write","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" \
  --tags “ddata”,”akka" --disk "name=akka-ddata-${node_number},device-name=akka-ddata-${node_number},mode=rw,boot=yes,auto-delete=yes"

done



gcloud compute --project "akka-gcp" instances create "akka-ddata-002" --zone "us-central1-a" \
  --machine-type "n1-standard-4" --subnet "default" \
  --no-address\
  --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring.write","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" \
  --tags "ddata","akka" --disk "name=akka-ddata-002,device-name=akka-ddata-002,mode=rw,boot=yes,auto-delete=yes"