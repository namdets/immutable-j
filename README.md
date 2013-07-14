immutable-j
================

Annotation library adding immutability enforcement for Java at the class 
level. 

Instances of a class marked @Immutable are ensured to be
proper stable values and references to them can be passed around at
will, provided that the ImmutableAnnotationProcessor actually runs
during compilation of the class.

Instances of a class marked @ImmutableTypeParameters are ensured to have
only generic type parameters of immutable classes.

The definition used for immutability related to this class comes
from the Oracle Java Tutorial on the subject:

http://docs.oracle.com/javase/tutorial/essential/concurrency/immutable.html

A softer definition appears to be used in JavaDoc for some built-in 
classes to enable some performance optimizations like memoization and 
lazy  initialization. These mechanisms are useful and in some cases provide 
significant performance benefits, however their use prevents us from 
programmatically ensuring that a class is in fact immutable.

I would call these built-in classes externally immutable. I have included
a list of the built-in classes I considered externally immutable which
may be useful in creating immutable objects. These classes are white-listed
as being immutable for use in classes marked by the @Immutable annotation.

If you would like some classes to be added to this list, please issue
a pull request.

The processor accepts that a class is immutable if every field 
meets the following requirements:

	Must be marked final.
	One of the following must be true:
		Field is of a primitive type.
		Field is of a class which is marked @Immutable
		Field is on the list of immutable built-in classes below.

Currently White-listed Built-in Immutable Classes:

		java.lang.String,
		java.lang.Boolean,
		java.lang.Integer,
		java.lang.Character,
		java.lang.Byte,
		java.lang.Short,
		java.lang.Double,
		java.lang.Long,
		java.lang.Float,
		java.math.BigDecimal,
		java.math.BigInteger,
		java.awt.Color,
		java.awt.Font,
		java.util.Locale,
		java.util.UUID,
		java.io.File,
		java.net.URL,
		java.net.URI,
		java.net.Inet4Address,
		java.net.Inet6Address,
		java.net.InetSocketAddress


v1.1 : 

    Also added new annotation and associated processing to enforce
    only generic type parameters on a class to be immutable. This
    is done by specifying an extends clause on the type parameter
    that is marked @Immutable or meets the criteria for immutability.

