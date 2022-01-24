# Common

This modules provides cross-cutting utility functions and base classes for cqf-ruler plugins.

## Guidelines

### Types of Utilities

In general, reusable utilities are separated along two different dimensions, Classes and Behaviors.

Class specific utilities are functions that are associated with specific class or interface, and add functionality to that class.

Behavior specific utilities allow the reuse of behavior across many different classes.

### Class Specific Utilities

Utility or Helper methods that are associated with a single class should go into a class that has the pluralized name of the associated class. For example, utilities for `Client` should go into the `Clients` class. The ensures that the utility class is focused on one aspect and allows for more readable code:

`Clients.forUrl("test.com")`

as opposed to:

`ClientUtilities.createClient("test.com")`

or, if you put unrelated code into the class, you might end up with something like:

`Clients.parseRegex()`

If the code doesn't read clearly after you've added an utility, consider that it may not be in the right place.

In general, all the functions for this type of utility should be `static`. No internal state should be maintained (`static final`, or immutable, state is ok). If you final that your utility class contains mutable state, consider an alternate design.

Examples

* Factory functions
* Adding behavior to a class you can't extend

### Behavior Specific Utilities

If there is behavior you'd like to share across many classes, model that as an interface and use a name that follows the pattern `"ThingDoer"`. For example, all the classes that access a database might be `DatabaseUser`. Use `default` interface implementations to write logic that can be shared many places. The interfaces themselves shouldn't have mutable state (again `static final` is ok). If it's necessary for the for shared logic to have access to state, model that as an method without a default implementation. For example:

```java
interface DatabaseUser {
   Database getDb();

   default Entity read(Id id) {
      return getDb().connect().find(id);
   }
}
```

In the above example any class that has access to a `Database` can inherit the `read` behavior.

Examples

* Cross-cutting concerns

### Discovery

Following conventions such as these make it easier for the next developer to find code that's already been implemented as opposed to reinventing the wheel.
