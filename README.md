# [CLSP](https://doi.org/10.1016/j.cie.2024.110634)
Capacitated Lotsizing and Scheduling Problem, published in Computers & Industrial Engineering journal as "Changeover minimization in the production of metal parts for car seats"


## Abstract
We tackle a capacitated lot-sizing and scheduling problem (CLSP) with the main objective of minimizing changeover time in the production of metal parts for car seats. Changeovers occur when a machine (or production line) is reconfigured to produce a different product or part, leading to production downtime and loss of efficiency. In this study, we first provide a mixed-integer programming (MIP) formulation of the problem. We test the limits of solving the problem with commercial mathematical programming software. We also propose two approaches to tackle instances found in practice for which the mathematical programming model is not a viable solution method. Both approaches are based on partitioning the entire production of a part into production runs (or work slots). In the first approach, the work slots are assigned to machines and sequenced by a metaheuristic that follows the search principles of the GRASP (greedy randomized adaptive procedure) and VNS (variable neighborhood search) methodologies. In the second approach, we develop a Hexaly Optimizer (formerly known as LocalSolver) model to assign and sequence work slots. The study provides insights into how to minimize changeovers and improve production efficiency in metal parts manufacturing for car seats. The findings of this study have practical implications for the auto-part manufacturing industry, where efficient and cost-effective production is critical to meet the demands of the market.

## Authors
Authors involved in this work:
- J. Manuel Colmenar
- Manuel Laguna
- Raúl Martín Santamaría

## Datasets
Instances are located inside the 'instances' folder. Each file has a comment section where the instance is explained.

## Compiling

You can easily compile and build an executable artifact of this project using Maven and a recent version of Java (17+):
```text
mvn clean package
```

## Executing

This project project requires the installation and configuration of Hexaly Optimizer (formerly known as LocalSolver) in your system. The software is free for academic purposes. However, it is possible to run the project without Hexaly by removing/commenting the ```hexaly``` package, and the corresponding dependency in the ```pom.xml``` file. 

To execute the project, you can just run the generated jar file in target, which includes the ```application.yml``` file already included in the ```resources``` directory. For easy of use there is an already compiled JAR inside the ```target``` folder.

To review a full list of configurable parameters, either using an ```application.yml``` in the same folder as the executable, or using command line parameters, see the [Mork documentation, section configuration](https://docs.mork-optimization.com/en/latest/features/config/).

The results of the execution will be stored in the ```results``` folder, with a timestamped file for each execution using Excel format. The corresponding solutions will be stored in the ```solutions``` folder.

## Instance format

Each instance follows this format (also explained as comments in the instance file):

```
J number of parts
K Number of machines
T Number of periods (weeks)
Production rate of part j in machine k, r[j,k] matrix. If a machine k is not able to produce part j, the value is 0.
Changeover time from part i to part j, c[i,j] matrix.
Inventory position of part j in period t matrix.
Capacity (number of hours) of machine k in period t matrix.
Priority for producing part j on machine k matrix. If p[k,j] = 0, then machine k is the preferred machine for part j.
--- If k is the second preferred machine for producing part j, then p[k,j] = 1, and so on.
A blank line is needed after the header.
```

## Cite

Consider citing our paper if used in your own work:

### DOI
[https://doi.org/10.1016/j.cie.2024.110634](https://doi.org/10.1016/j.cie.2024.110634)

### Bibtex
```bibtex
@article{COLMENAR2024110634,
title = {Changeover minimization in the production of metal parts for car seats},
journal = {Computers & Industrial Engineering},
pages = {110634},
year = {2024},
issn = {0360-8352},
doi = {https://doi.org/10.1016/j.cie.2024.110634},
url = {https://www.sciencedirect.com/science/article/pii/S0360835224007563},
author = {J. Manuel Colmenar and Manuel Laguna and Raúl Martín-Santamaría},
keywords = {Lot sizing, Multi-period production scheduling, Nonidentical parallel machines, Metaheuristic optimization},
abstract = {We tackle a capacitated lot-sizing and scheduling problem (CLSP) with the main objective of minimizing changeover time in the production of metal parts for car seats. Changeovers occur when a machine (or production line) is reconfigured to produce a different product or part, leading to production downtime and loss of efficiency. In this study, we first provide a mixed-integer programming (MIP) formulation of the problem. We test the limits of solving the problem with commercial mathematical programming software. We also propose two approaches to tackle instances found in practice for which the mathematical programming model is not a viable solution method. Both approaches are based on partitioning the entire production of a part into production runs (or work slots). In the first approach, the work slots are assigned to machines and sequenced by a metaheuristic that follows the search principles of the GRASP (greedy randomized adaptive procedure) and VNS (variable neighborhood search) methodologies. In the second approach, we develop a Hexaly Optimizer (formerly known as LocalSolver) model to assign and sequence work slots. The study provides insights into how to minimize changeovers and improve production efficiency in metal parts manufacturing for car seats. The findings of this study have practical implications for the auto-part manufacturing industry, where efficient and cost-effective production is critical to meet the demands of the market.}
}
```
