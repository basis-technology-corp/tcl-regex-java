# TCL/HSRE port to Java #

## Notes on the Port ##

Compare to rlp/tclregex. The tclgrep command is handy for parallel debugging; it sometimes needs some 
editing to pass the right flags.

The compiler in C is regcomp.c and regc_lex.c. The compiler code is, by and large, in Compiler, with the 
lexical analysis in Lex.

The color map (regc_color.c) is ColorMap and ColorDesc, with some code falling into 
Locale. The Unicode range support comes from com.ibm.icu.text.UnicodeSet. Note that the C code from HSRE has 
a set of stubs for something called multi-character collating elements. These are all stubbed out in the 
TCL version that we incorporate in RLP, and that's fine.

The compiler's job is to build NFA objects, which internally are composed of Arc and State objects. 
Once the whole regex is represented as an NFA, it is compacted into a 'Cnfa'. Cnfa represents arcs a `long` 
values that pack a color and a target. States are just integer indices into the array of arcs.

The runtime takes the Cnfa and decomposes it, on the fly, into one or more Dfa objects. (C code is a 
mixture of regexec.c and rege_dfa.c). This is how the thing avoids looping infinitely, by breaking the 
problem into smaller DFA problem.

In Java, the runtime is a combination of Runtime, RegExp, and some data stored in Guts. This is a reflection
of the C code structure; over time we should be evolving all of this into the RegExp class, I think.


