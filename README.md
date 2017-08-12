WALA MemSAT
===========

 This is the code for the memory model checker MemSAT published at PLDI 10, and also for the Miniatur Java code checker published in FSE 07.  The code comes with the tests reported in the PLDI paper and some additional sequential tests to illustrate Miniatur functionality.

 To build the system, the easiest way is to start with the team project set at the root of the repository, memsat.psf and import that into a fresh Eclipse workspace; Eclipse Luna is known to work, but older versions may function too.

 After the import finishes, you will need to run the various build scripts provided to fetch needed libraries that are not shipped with WALA MemSAT.  Specifically, run 'ant' in the root directory or each of the following projects:

com.ibm.wala.cast
com.ibm.wala.cast.java 
com.ibm.wala.cast.java .ecj
com.ibm.wala.cast.js
com.ibm.wala.cast.js.rhino
com.ibm.wala.memsat

 Note that you may have to 'refresh' these projects in Eclipse before the added libraries will be seen.

 After this, you should be set to use MemSAT.  To see that everythig was built correctly, try running the tests in the launcher com.ibm.wala.memsat.test/launchers/com.ibm.miniatur2.tests.launch.  Note that you may have to adjust the JRE setting, since I have so far been unable to make that properly portable.  There are also tests using the JDT integration of WALA in com.ibm.wala.memsat.jdt.test/launchers/com.ibm.wala.memsat.jdt.test.launch
 

