#!/bin/bash

set -x

args=""

for i in ${@:3}
do
    args="$args $i"
done

spark-submit \
    --class $2 \
    --packages com.databricks:spark-avro_2.11:3.2.0 \
    --master yarn \
    --deploy-mode cluster \
    --jars /home/lsde06/libs/libadsb-2.1.1.jar \
    $1 $args
