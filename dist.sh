sbt clean compile assembly
rm -rf dist
mkdir dist
cp src/main/resources/* dist/
cp target/akkastuffs.jar dist/
chmod 777 dist/cluster-admin
