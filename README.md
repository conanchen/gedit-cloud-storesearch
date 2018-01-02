- skip test
  1. $ gradle build -x test
  2. $  

- Install ElasticSearch
    1. https://www.elastic.co/guide/en/elasticsearch/reference/6.1/docker.html#_image_types
    2. $ docker pull docker.elastic.co/elasticsearch/elasticsearch:6.1.1
    3. Development mode
        -  $ docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.1.1
        - $ mkdir /Users/conanchen/elasticsearch/data
        - $ chmod g+rwx /Users/conanchen/elasticsearch/data
        - $ chgrp 1000 /Users/conanchen/elasticsearch/data
        - $ cd elasticsearch; 
        - $ docker-compose up
        - $ docker-compose down
  