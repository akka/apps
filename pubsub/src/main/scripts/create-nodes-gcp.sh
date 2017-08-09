for node_number in 020 021 022 023 024 025 026 027 028 029 030 031 032 033 034 035 036 037 038 039 040 041 042 043 044 045 046 047 048 049
do
  echo "Creating new node akka-pubsub-${node_number}..."
  gcloud compute --project "akka-gcp" disks create "akka-pubsub-${node_number}" --size "10" --zone "us-central1-a" --source-snapshot "akka-pubsub" --type "pd-standard"

  gcloud compute --project "akka-gcp" instances create "akka-pubsub-${node_number}" --zone "us-central1-a" \
  --machine-type "n1-standard-4" --subnet "default" \
  --no-address\
  --maintenance-policy "MIGRATE" --service-account "7250250762-compute@developer.gserviceaccount.com" \
  --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring.write","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" \
  --tags "pubsub","akka" --disk "name=akka-pubsub-${node_number},device-name=akka-pubsub-${node_number},mode=rw,boot=yes,auto-delete=yes"

done
