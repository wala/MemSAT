WALA MemSAT [![Build Status](https://travis-ci.org/wala/MemSAT.svg?branch=master)](https://travis-ci.org/wala/MemSAT)
===========

 This is the code for the memory model checker MemSAT published at PLDI 10, and also for the Miniatur Java code checker published in FSE 07.  The code comes with the tests reported in the PLDI paper and some additional sequential tests to illustrate Miniatur functionality.

The MemSAT build process to use is the one that works on Travis CI; it uses Maven, and has two steps:

   * You need to have built the latest WALA by cloning it and doing a 'mvn clean install -DskipTests'. MemSAT relies on WALA projects and Maven expects to find them installed.

  * 'mvn clean install' in the top-level directory of the clone of MemSAT
