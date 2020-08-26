# CommentConfig

A project that utilizes SnakeYAML and offers the ability to save comments to a YAML file.

This project is dependent on [SnakeYAML](https://github.com/asomov/snakeyaml) and is guranteed to function on version 1.26 (however not guranteed for other versions).

## Building
Just run `mvn clean package` to build the jar. 

## Basic Usage
CommentConfig offers two different methods of creating custom YAML files.

For both methods let's say we want to create a YAML file that looks like:
```yaml
# The COOKIE Config

# How many cookies do you want to eat?
num-cookies: 10
# Properties for the cookie
cookie:
  # The type of cookie: chocolate, oreo, oatmeal.
  type: chocolate
  # The size of the cookie: big or small
  size: big
  
```

### Using the `ConfigNode` structure
 The first way is through the `ConfigNode` objects which form a tree structure that allow navigation
throughout the levels of the YAML file. 
We would start by creating a root node that will be the top level of our tree like so:
`ParentConfigNode root = ParentConfigNode.createRoot();`. Then from there we can start adding children to the tree.
We add a child like this: `root.addChild(String childName, Object value)` and that will return a `ValueConfigNode`. 
We also want to add comments though as well and each `ConfigNode` object allows us to add comments to it by calling `node.setComments(String...)`. 
The first value we will add is our `num-cookies` property. We can add it like this:
```java
ParentConfigNode root = ParentConfigNode.createRoot();
root.addChild("num-cookies", 10).setComments("How many cookies do you want to eat?");
```
It's as simple as that! Now let's add the cookie section with the type and size property. To add a section we simply use `node.addSection(String sectionName)`.
That will return a `ParentConfigNode` which we can add more children and more sections to. Let's go ahead and do that.
```java
ParentConfigNode cookieProperties = root.addSection("cookie");
// Let's add our comment to the section as well.
cookieProperties.setComments("Properties for the cookie");
// Let's go ahead and add the children to the properties
cookieProperties.addChild("type", "chocolate").setComments("The type of cookie: chocolate, oreo, oatmeal.");
cookieProperties.addChild("size", "big").setComments("The size of the cookie: big or small");
```
Now last, but not least, let's add our header comment to the root as well (the cookie config one): `root.setComments("The COOKIE config!", "");`. Notice the empty `""` to give
an empty line between the header and the rest of the comments.
That's it for method 1. We'll talk about how to serialize a `ConfigNode` below.

### Using the ORM
CommentConfig has a very simple ORM (object-relational mapping) that utilizes SnakeYAML to convert field values to the YAML properties. The default serializer and deserializer can
cover all primitive values, but it's not very extensive. It's still more than enough for most configs though! Let's take a look at how we can use the ORM.
First, we'll need to create a class representation of our config; let's call it `CookieConfig`!. The code below will contain everything the class needs, and we'll go over it after.
```java
@SerializableConfig
@Comment({"The COOKIE config", ""})
public class CookieConfig {
  @Node("num-cookies")
  @Comment("How many cookies do you want to eat?")
  private int numCookies = 10;
  
  @Node("cookie")
  @Comment("Properties for the cookie")
  private CookieProperties cookieProps = new CookieProperties();
  
  @SerializableConfig
  private class CookieProperties {
    @Comment("The type of cookie: chocolate, oreo, oatmeal.")
    public String type = "chocolate";

    @Comment("The size of the cookie: big or small")
    public String size = "big";
  }
}
```
Each class (including inner classes) that should be serialized by the ORM should be marked with `@SerializableConfig`. The two most prominent annotations throughout this example are
`@Node` and `@Comment`. It's pretty easy to guess what `@Comment` does. It indicates what comments should go on the field. The `@Node` annotation is used to define the path of the
node in the YAML file as well as the key. By default the ORM will use the field name as the key in the YAML file, so the annotation is useful when we want to store it by
another name. `@Node` accepts a string array, which will be the path. The last value of the array will be the key under which the field is stored. For example if a field was
marked with `@Node({"level1", "key"})` the field would be stored as
```yaml
level1:
  key: ...
```
In the class example above, we used the `CookieProperties` subclass to represent another level in our config, which is a nice feature to keep that data structured in code as well.

#### Serializing the Class
Once a serializable class is created, it can be converted into a `ParentConfigNode` by simply doing `ClassSerializer.serializeClass(Object obj)` where `obj` is an instance of
the serializable class.

### Serializing a `ConfigNode`
You can convert a config node to a YAML string by creating an instance of `NodeSerializer`. For example let's say we had a `ParentConfigNode`, we can convert it to a YAML string like:
```java
ParentConfigNode root = ...;
NodeSerializer serializer = new NodeSerializer();
String producedYAML = serializer.serializeToString(root);
```
Then you can write that string to a file.

### Deserializing a class
Unfortunately it's not possible to convert the YAML string back to a `ParentConfigNode` because there is no record about what type of value was stored before.
However, it is a possible to deserialize the YAML back to a `@SerializableConfig` class using the ORM. To do that simply create a new instance of `ClassDeserializer` and call
the `deserializeClass` passing in the yaml string or file and the class to deserialize it as.
