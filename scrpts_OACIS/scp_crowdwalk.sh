
remote_dir=~/Programs/crowdwalk

for i in 00 01 02 03
do
  scp -i ~/.ssh/cassia${i}_rsa -p crowdwalk_oacis.rb cassia${i}a:$remote_dir
  scp -i ~/.ssh/cassia${i}_rsa -p file_generator.rb cassia${i}a:$remote_dir
  #scp -i ~/.ssh/cassia${i}_rsa -p sample/kamakura/2014_0109_kamakura11-3_widthnarrow4.xml cassia${i}a:$remote_dir/sample/kamakura/
  #scp -i ~/.ssh/cassia${i}_rsa -p sample/kamakura/2014_0109_kamakura11-3_widthnarrow1_2_2_4.xml cassia${i}a:$remote_dir/sample/kamakura/
done
