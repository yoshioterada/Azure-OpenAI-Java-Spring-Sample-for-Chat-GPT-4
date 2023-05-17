# How to evaluate the Sample App.

## Perparation 

* Install Java 17 or latest

# Run the Application

1. git clone https://github.com/yoshioterada/Azure-OpenAI-Java-Spring-Sample-for-Chat-GPT-4.git
2. Open [./src/main/java/com/yoshio3/SSEOpenAIController.java](./src/main/java/com/yoshio3/SSEOpenAIController.java) 
3. Modified the following　code  
  please replace the String to your own Azure OpenAI Access Key.

  ```java
  private final static String OPENAI_API_KEY = "*******************************"; 
  ```

4. Execute the Maven command

```bash
   ./mvnw spring-boot:run
```

5. Open the Browser and Access to http://localhost:8080。
