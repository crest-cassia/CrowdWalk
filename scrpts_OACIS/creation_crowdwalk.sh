sim_json=crowdwalk_kamakura_wide_2500persons_test_link_loop.json
sim_id_json=crowdwalk_kamakura_wide_2500persons_test_link_loop_id.json

# create host list
bin/oacis_cli show_host -o host.json
echo "create host.json"

# create simulator (registration)
bin/oacis_cli create_simulator -h host.json -i $sim_json -o $sim_id_json

# create parameter set ids
echo "create parameter ids"
for i in 00 01 02 03
do
  bin/oacis_cli create_parameter_sets -s $sim_id_json -i parameter_sets_$i.json -o parameter_set_ids_$i.json
done

# create job
echo "job creation"
host_ids=(526520a56f7236e4a7000002 526523116f723687d4000004 526524c86f72369cba000005 526526086f7236513a000006)
#host_ids=(526526086f7236513a000006)
ids=(00 01 02 03)
#for i in 0 1 2 3; do; echo ${host_ids[$i]}, ${ids[$i]}; done
for i in  0 1 2 3
do
 bin/oacis_cli job_parameter_template -h ${host_ids[$i]} -o job_parameter_${ids[$i]}.json
 #bin/oacis_cli job_parameter_template -h 526526086f7236513a000006 -o job_parameter_03.json
done


# create runs
echo "simulation runs creation"
for i in 00 01 02 03
do
  bin/oacis_cli create_runs -p parameter_set_ids_$i.json -j job_parameter_$i.json -n 1 -o run_ids_$i.json
done
