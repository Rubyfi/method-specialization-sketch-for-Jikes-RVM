## Method specialization sketch for Jikes RVM

This repository contains code for an implementation of method specialization for Jikes RVM that I wrote in 2012. The code is incomplete. I was originally planning to do some more work on this but I've come to the conclusion that I would rather spend the time on something else. I'm publishing this as it is; some parts of the code might be useful for someone else.

I've removed the development history by squashing the commits. The original work was done in a Mercurial repository and I didn't want to spend time migrating the history to Git.

# What's implemented?
The code implements method specialization as a compiler optimization, i.e. method specialization in the spirit of papers such as "Selective specialization for object-oriented languages" by Dean et al. or "Design and Evaluation of Dynamic Optimizations for a Java Just-In-Time Compiler" by Suganuma et al.

The implementation in this repository is much less sophisticated and more buggy, of course. It provides parameter profiling of baseline compiled application methods on IA32 32-bit via listeners and allows specialization of methods on exactly one non-receiver parameter method.

# Building
Works the same as a normal Jikes RVM. If you're building with require.rvm-unit-tests=true, please note that I've added specialization tests to OptTestHarnessTest. This has significantly increased the runtime for that test. For example, on prototype-opt on an old IA32 machine, the OptTestHarnessTest alone might take 1 minute.

# License
The code is provided to you under the Eclipse Public License, the same license that the Jikes RVM uses.

# Support
As described above, I don't plan to do further development on this so the code is unsupported.

# References
In addition to the references mentioned above, I found the following references interesting (your mileage may vary):
* "Runtime value specialization" by Panagiota Bilianou. The literature section covers most of the relevant works. The thesis also contains a chapter about possible integration of specialization with the adaptive optimization system. The thesis is available for free after registration via the electronic thesis online service of the British Library. See http://ethos.bl.uk/OrderDetails.do?uin=uk.bl.ethos.549159 .
* "Constraint based optimization of stationary fields" by Rogers et al.
* ("A methodology for procedure cloning" (1993) and "Procedure cloning"(1992) by Cooper et al.

# Original Jikes RVM readme follows

## Jikes Research Virtual Machine

Jikes RVM (Research Virtual Machine) provides a flexible open testbed to prototype virtual machine technologies and experiment with a large variety of design alternatives. The system is licensed under the [EPL](http://www.eclipse.org/legal/epl-v10.html), an [OSI](http://www.opensource.org/) approved license. Jikes RVM runs on IA32 32 bit (64 bit support is work in progress) and PowerPC (big endian only).

A distinguishing characteristic of Jikes RVM is that it is implemented in the Java™ programming language and is self-hosted i.e., its Java code runs on itself without requiring a second virtual machine. Most other virtual machines for the Java platform are written in native code (typically, C or C++). A Java implementation provides ease of portability, and a seamless integration of virtual machine and application resources such as objects, threads, and operating-system interfaces.

More information is available at our [website](http://www.jikesrvm.org).

## Building

You'll need

* a JDK (>= 6)
* Ant (>= 1.7) with optional tasks
* GCC with multilibs
* Bison
* an internet connection during the first build to download [GNU Classpath](http://www.gnu.org/software/classpath/) and other components

Please see the [user guide](http://www.jikesrvm.org/UserGuide/) for more details.

## Need support?

Please ask on the researchers [mailing list](http://www.jikesrvm.org/MailingLists/).

## Bug reports

If you want to report a bug, please see [this page on our website](http://www.jikesrvm.org/ReportingBugs/).

## Contributions

See the [contributions page](http://www.jikesrvm.org/Contributions/) for details. 

The short version:

* Contributions are licsensed under EPL and require a Contributor License Agreement. You keep your copyright.
* You can send us patches or use pull requests. Send patches to the [core mailing list](mailto:jikesrvm-core@lists.sourceforge.net).
* It is ok to test on one platform only (e.g. only on IA32).

## History

The project migrated from Subversion to Mercurial and from Mercurial to Git. Certain older changes are not contained in this repository. If you need access to these changes, you can browse the old repositories at [SourceForge](http://sourceforge.net/p/jikesrvm). The relevant parts of the old repositories are also mirrored on GitHub (see below).

The last commit in the Mercurial repository is commit #11358 (hg commit id d4ced37a7a0d) from Tue, 08 Sep 2015 13:55:48 +0200. The matching commit in this Git repository has the commit id 871ee0e826c161c8cb99bba7280dced6da850779.

The last interesting commit in the Subversion repository is commit #16061 (Move assertion on heavy lock state to within lock mutex to avoid possible race with inflation code). The matching commit in this Git repository has the commit id 164e4f465640364da0b135b78307e8cf1de8a070. The very last commit in the Subversion repository is commit #16068 (disable runs on piccolo until we get hg working on AIX.).

Mirrors of the old repositories on GitHub:

* The main code is mirrored at https://github.com/JikesRVM/mirror-historical-svn-jikesrvm . Please note that the mirror includes old branches which aren't present in this repository.
* Tuningfork is mirrored at https://github.com/JikesRVM/mirror-historical-svn-tuningfork
* Cattrack, an application for tracking test results that the project used in the past, is mirrored at https://github.com/JikesRVM/mirror-historical-svn-cattrack
