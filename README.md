# rubberband
Attensa ElasticSearch java APIs.

This is definitely a work-in-progress.  Would love pull requests to make it better and more feature complete!

Access via maven: 

```
<dependency>
    <groupId>com.attensa</groupId>
    <artifactId>rubberband</artifactId>
    <version>0.0.2</version>
</dependency>
```

Sample usage:
```java

    @Value
    public class Cat {
        String id;
        String name;
        String gender;
        String breed;
        String description;
    }


    public static void main(String[] args) throws InterruptedException {
        Gson gson = new Gson();
        Retryer<Response> retryer = new Retryer<>(StopStrategies.stopAfterAttempt(2), WaitStrategies.exponentialWait(), Attempt::hasException);
        HttpTemplate httpTemplate = new HttpTemplate(HttpClientBuilder.create().build(), gson, retryer);
        RubberbandClient client = new RubberbandClient(httpTemplate, gson, "http://localhost:9200");

        Cat winston = new Cat("1", "Winston", "male", "Turkish Van", "A very fluffy, friendly cat.");
        Cat templeton = new Cat("2", "Templeton", "male", "American Shorthair", "The old man of the family. Black, and always hungry.");
        Cat zipper = new Cat("3", "Zipper", "female", "Longhair", "Skittish mama of Brownie.");
        Cat brownie = new Cat("4", "Brownie", "female", "Longhair", "Zipper's daughter. The least friendly of the crew. Hates the boys and yowls at them often.");
        List<Cat> cats = newArrayList(templeton, zipper, brownie);

        client.deleteIndex("animals");

        client.createIndex("animals", createCatMappings());

        //save a single one
        client.save("animals", "cat", winston.getId(), winston);
        //save a few using the bulk API
        client.save("animals", "cat", seq(cats).toMap(Cat::getId, c -> c));

        SearchRequest searchRequest = new SearchRequest(new QueryStringQuery("gender: male", null, "_all"), null, null);

        //loop for a bit because ES takes a bit to make saved items searchable, after indexing.
        waitForResultsToShowUp(client, searchRequest, 2);

        Page<Cat> result = client.query("animals", searchRequest, new PageRequest(20, 0), Cat.class);
        result.getContents().forEach(System.out::println);

        client.delete("animals", "cat", winston.getId());
        waitForResultsToShowUp(client, searchRequest, 1);
        System.out.println("After deletion:");
        result = client.query("animals", searchRequest, new PageRequest(20, 0), Cat.class);
        result.getContents().forEach(System.out::println);

        Optional<Cat> savedZipper = client.get("animals", "cat", zipper.getId(), Cat.class);
        System.out.println("savedZipper = " + savedZipper);

        Optional<Cat> deleted = client.get("animals", "cat", winston.getId(), Cat.class);
        System.out.println("deleted = " + deleted);
    }

    private static ElasticSearchMappings createCatMappings() {
        Config descriptionOptions = new Config("string", null, true, "english", null, null);
        Map<String, Config> properties = new HashMapBuilder<String, Config>()
                .with("description", descriptionOptions)
                .build();
        Map<String, PropertyContainer> typeMapping = Collections.singletonMap("cat", new PropertyContainer(properties));
        return new ElasticSearchMappings(typeMapping, null);
    }

    private static void waitForResultsToShowUp(RubberbandClient client, SearchRequest searchRequest, int expected) throws InterruptedException {
        long count;
        int tries = 0;
        do {
            count = client.count("animals", searchRequest);
            Thread.sleep(200);
        } while (count != expected && tries++ < 10);
        System.out.println("number of results = " + count);
    }

```
