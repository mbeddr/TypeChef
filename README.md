** For the original README refer to https://github.com/ckaestne/TypeChef**

TypeChef
========

[![Build Status](https://travis-ci.org/ckaestne/TypeChef.svg?branch=master)](https://travis-ci.org/ckaestne/TypeChef)
[![Coverage](https://coveralls.io/repos/ckaestne/TypeChef/badge.png?branch=master)](https://coveralls.io/github/ckaestne/TypeChef)

**For instructions on how to import the project using IntelliJ follow the instructions at the end**

TypeChef is a research project with the goal of analyzing 
ifdef variability in C code with the goal of finding
variability-induced bugs in large-scale real-world systems,
such as the Linux kernel with several thousand 
features (or configuration options).

Instead of analyzing each variant for each feature 
combination in isolation, TypeChef parses the
entire source code containing all variability in a 
variability-aware fashion without preprocessing.
The resulting abstract syntax tree contains the 
variability in form of choice nodes. Eventually, a
variability-aware type system performs type checking 
on these trees, variability-aware data-flow analysis
performs data-flow analysis and so forth.

TypeChef was started with the goal of building a type
system for C code with compile-time configurations.
TypeChef was originally short for
*Type Checking Ifdef Variability*.
Over time it has grown into an infrastructure of all
kinds of analyses.
In all cases, the goal is to detect errors in all possible 
feature combinations, without resorting to a brute-force approach. 
It also evolved into a basis for a sound transformation and refactoring engine
([Hercules](https://github.com/joliebig/Hercules) and
[Morpheus](https://github.com/joliebig/Morpheus)) and as 
an import mechanism for [mbeddr](https://github.com/mbeddr).


<a href="http://ckaestne.github.com/TypeChef/typechef-poster.png"><img alt="TypeChef Poster" src="http://ckaestne.github.com/TypeChef/typechef-poster-small.png" /></a>

Architecture and Subprojects
----------------------------

The TypeChef project contains of four main components 
and several helper libraries.

* A library for reasoning about **feature expressions** (subproject *FeatureExprLib*). 
  The library allows to easily express and reason about expressions
  in propositional logic. It supports also parsing feature expressions
  and loading entire feature models (in textual format or a .dimacs file).
  For reasoning, internally both BDDs and SAT solvers are used which
  allows to scale reasoning even to feature models the size of the Linux kernel.
  The library is stable, has a simple and convenient syntax, and can be (and is) reused in 
  very different contexts. It also works well with Java code.

* A library of **conditional data structures** for variational programming 
  (subproject *ConditionalLib*) with several useful operations on
  conditional structures. Used heavily in all other subprojects.
  For a short introduction see [VariationalProgramming.md](https://github.com/ckaestne/TypeChef/blob/master/VariationalProgramming.md).

* A **variability-aware lexer** (also called partial preprocessor;
  subproject *PartialPreprocessor*) that
  reads unpreprocessed code and produces a conditional 
  token stream. The variability-aware lexer is responsible
  for resolving macros and file inclusions and for 
  normalizing `#ifdef` conditions. 
  There are two possible internal implementations to chose from,
  our own based on a heavily modified version of [jcpp](http://www.anarres.org/projects/jcpp/) and 
  the independently developed on from [xtc](http://cs.nyu.edu/xtc/).

* A **variability-aware parser framework** provides 
  parser combinators to build variability-aware parsers
  (subproject *ParserFramework*).

* The **variability-aware parsers** for GNU C and Java 
  (subprojects *CParser* and *JavaParser*) use the parser
  framework to build parsers for the corresponding languages. 
  The parsers read a conditional token stream and
  produce abstract syntax trees with corresponding choice nodes.
  Variability-aware parsers for HTML and JavaScript exist in
  forks.

* A **variability-aware type system** (subproject *CTypeChecker*)
  checks types considering variability in the abstract syntax tree. 
  As a normal type system, it walks over the AST, collects an environment
  of known types, and issues (conditional) type errors when problems
  are found. In addition, it extracts (conditional) symbol tables 
  needed for linker checks.

* A **variability-aware control-flow and data-flow analysis**
  (subproject *CRewrite*) provides implementations for
  successor/predecessor determination of abstract syntax tree
  elements in the presence of choice nodes and on top of it
  a variable liveness implementation. 

* A **call graph** analysis with a corresponding **pointer analysis**
  is currently developed in a [fork](https://github.com/gabrielcsf/TypeChef).

* A **rewrite and refactoring engine** built on TypeChef is
  available as separate projects [Hercules](https://github.com/joliebig/Hercules) and
  [Morpheus](https://github.com/joliebig/Morpheus).

* Setups for analyzing individual systems together with useful tooling  are available as
  separate github projects (see section evaluation below).



Importing as a Project in IntelliJ 2017.1
-----------------------------------------


* Clone the repository and install the 2017.1 version of IntelliJ (latest as of 11.04.2017) 

* Open IntelliJ and make sure SCALA is enabled in the Plugins section. You can do this either by opening INtelliJ and clicking on File-> Settings-> Plugins or by simply using the Configure on the bottom right of the opening Dialog when you open IntelliJ for the first time. In the Plugin Search box, type in "Scala" and make sure the checkbox next to Scala is checked. **You will be required to restart IntelliJ after this.** 

* If SCALA is not installed, then click on "Install Jetbrains Plugins" in the same Plugin Dialog box and choose SCALA

> If you are using a Windows machine, make sure all Firewall is turned off. Otherwise, Windows firewall may prevent Intellij from being able to contact the Jetbrains repository. It is also recommended that you uninstall any existing installation of SCALA and use the IntelliJ plugin shipped version of SCALA to avoid any conflicts.

* After installing SCALA in your IntelliJ and restarting IntelliJ, choose the "Import Project" option. 

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/opsnProject1.PNG"><img  width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/opsnProject1.PNG" /></a>

* Choose the source directory of the cloned repository

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/oprnProject2.PNG"><img width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/oprnProject2.PNG" /></a>

* In the next dialog, choose "Import Projects from external model" and then choose "SBT"

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/openProject3.PNG"><img width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/openProject3.PNG" /></a>

* In the next dialog, make sure both the "Library sources" and the "SBT Sources" are checked and make sure the Project format is ".ipr(file based)"

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/openProject4.PNG"><img width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/openProject4.PNG" /></a>

* After a few minutes of waiting, In the next menu, you will be asked to choose the modules to load. Choose all. The next step will take a few minutes to load all the required JARs. Be patient.

* Once the loading is complete, Build the project. This should take a while too when you are doing it for the first time. 

* If your build fails because of issue with dependencies related to Partialcodechecker from mbeddr's project, goto the module settings of the PartialCodeChecker and mark the "src" folder as "Sources"

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/errorMbeddrPartialCodeCheckerNotFound.PNG"><img width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/errorMbeddrPartialCodeCheckerNotFound.PNG" /></a>

<a href="https://github.com/mbeddr/TypeChef/blob/master/README_files/solnMbeddrPartialCodeCheckerNotFound.PNG"><img width="50%" height="50%" alt="Open Project 1" src="https://github.com/mbeddr/TypeChef/blob/master/README_files/solnMbeddrPartialCodeCheckerNotFound.PNG" /></a>

* Once the import of project and successful build of project is done, close your IntelliJ and open the TypeChef.ipr file in a text editor and paste the contents of the file README_files/artifactdefinition.md as the first **<component>** in the ipr xml tree. Ideally, you should paste it between the line that looks like **<project version=....>** and **<component name=...>**. 
