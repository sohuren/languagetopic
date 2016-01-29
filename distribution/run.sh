#source ~/.bash_profile
cp ../bin/mctest.jar .
java -XX:+UseParallelGC -XX:-UseConcMarkSweepGC -Xmx3000m -Xms1024m -Djava.library.path=".:${default_lib_path}" -jar mctest.jar
