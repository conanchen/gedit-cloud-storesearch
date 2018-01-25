- skip test
  1. $ gradle build -x test
  2. $  

- Install ElasticSearch
    1. https://www.elastic.co/guide/en/elasticsearch/reference/6.1/docker.html#_image_types
    2. $ docker pull docker.elastic.co/elasticsearch/elasticsearch:6.1.1
    3. Development mode
        -  $ docker run -p 19200:9200 -p 19300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.1.1
        - $ mkdir /Users/conanchen/elasticsearch/data
        - $ chmod g+rwx /Users/conanchen/elasticsearch/data
        - $ chgrp 1000 /Users/conanchen/elasticsearch/data
        - $ cd elasticsearch; 
        - $ docker-compose up
        - $ docker-compose down
  
  
[https://docs.docker.com/get-started/part2/#run-the-app]
1. Build the local app image 
    - $ docker build -t gedit-cloud-storesearch .
    - $ docker run -p 18088:8088 -p 18980:8980 gedit-cloud-storesearch
2. [https://docs.docker.com/get-started/part2/#share-your-image] 
3. Pull and run the image from the remote repository
	- $ docker run -p 18088:8088 -p 18980:8980 conanchen/gedit-cloud-storesearch:latest


- skip test
  - $ gradle build -x test
  - $  
  
    