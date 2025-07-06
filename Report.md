**Question 1a:**What are the pros and cons of using a plain string like `"LOGIN|user|pass"`?

**Answer:** 

This method is simple and readable, but it is not suitable for complex data. Using plain strings for client-server data transfer can also lead to errors and processing issues.

**Question 1b:**How would you parse it, and what happens if the delimiter appears in the data?

**Answer:**

You can extract the data using functions like ‍split and replace, but if the delimiter character exists within the data, it may cause incorrect transmission or storage, potentially leading to serious errors.

**Question 1c:**Is this approach suitable for more complex or nested data?

**Answer:**

No, this approach is not ideal for complex data, as extracting information becomes difficult and inefficient.

**Question 2a:**What’s the advantage of sending a full Java object?

**Answer:**

This ensures that data is transmitted accurately without modification.

**Question 2b:**

**Answer:**

It might be possible, but it would be challenging, as handling this format between Python and Java is not straightforward.

**Question 3a:**

**Answer:**

It is widely used due to its simple structure and readability.

**Question 3b:**

**Answer:**

Yes, JSON is language-agnostic.








