package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.neighborhood.Neighbor;
import jobshop.solvers.neighborhood.Neighborhood;
import jobshop.solvers.neighborhood.Nowicki;

import java.util.Arrays;
import java.util.List;

public class TabooSolver implements Solver {

    final Neighborhood<ResourceOrder> neighborhood;
    final Solver baseSolver;
    final int maxiter;
    final int dureeTaboo;

    public TabooSolver(Neighborhood<ResourceOrder> neighborhood, Solver baseSolver,int maxiter,int dureeTaboo){
        this.neighborhood = neighborhood;
        this.baseSolver= baseSolver;
        this.maxiter=maxiter;
        this.dureeTaboo=dureeTaboo;
    }

    @Override
    public Result solve(Instance instance, long deadline){
        //sauvegarde l'information sur les permutation qu'on peut ou peut pas utiliser
        int [][] Taboo=new int[instance.numTasks*instance.numJobs][instance.numTasks*instance.numJobs];
        //pour les itérations
        int k=0;
        //pour garder le meilleur makespan
        int makespan;
        //on crée le ressource order de la solution de base
        ResourceOrder base=new ResourceOrder(baseSolver.solve(instance,deadline).schedule.get());


        //on remplit Taboo, initialement aucune permutation est Taboo
        for (int i=0;i<instance.numTasks;i++){
            for (int j=0;j< instance.numTasks;j++){
                Taboo[i][j]=0;
            }
        }

        //on boucle sur les voisins
        while (k<maxiter) {
            makespan= Integer.MAX_VALUE;
            //une itération en plus
            k++;
            //On utilise les swaps pour gérer plus facilement
            List<Nowicki.Swap> swaps=new Nowicki().allSwaps(base);
            System.out.println("On a "+swaps.size()+" swaps possibles");
            //pour choisir le meilleur voisin
            Nowicki.Swap bestswap=swaps.get(0);


            for (Nowicki.Swap currentswap : swaps) {
                //On vérifie si on a le droit d'utiliser le swap
                Task try1=base.getTaskOfMachine(currentswap.machine, currentswap.t1);

                Task try2=base.getTaskOfMachine(currentswap.machine, currentswap.t2);

                //si swipe pas Taboo
                if (Taboo[try1.job*instance.numTasks+ try1.task][try2.job*instance.numTasks+ try2.task]<=k && Taboo[try2.job*instance.numTasks+ try2.task][try1.job*instance.numTasks+ try1.task]<=k) {
                    System.out.println("on Rentre");
                    System.out.println(Taboo[try2.job*instance.numTasks+ try2.task][try1.job*instance.numTasks+ try1.task]);
                    System.out.println(Taboo[try1.job*instance.numTasks+ try1.task][try2.job*instance.numTasks+ try2.task]);
                    System.out.println("Swipe en question: "+try1+" et "+try2);
                    //On choppe le meilleur swap
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);
                    if (!base.toSchedule().isEmpty()) {
                        if (makespan > base.toSchedule().get().makespan()) {
                            makespan = base.toSchedule().get().makespan();
                            bestswap = currentswap;
                        }
                    }
                    //on unapply le swap
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);
                }
            }
            //on rajoute du temps dans taboo pour les task swapper
            Task task1=base.getTaskOfMachine(bestswap.machine, bestswap.t1);
            Task task2=base.getTaskOfMachine(bestswap.machine, bestswap.t2);
            //On utilise les identifiants des tasks permutés en indice
            Taboo[task1.job*instance.numTasks+ task1.task][task2.job*instance.numTasks+ task2.task]=k+dureeTaboo;
            System.out.println("update : "+Taboo[task1.job*instance.numTasks+ task1.task][task2.job*instance.numTasks+ task2.task]);
            //on applique le meilleur swap pour aller dans la prochaine boucle
            base.swapTasks(bestswap.machine, bestswap.t1, bestswap.t2);
            System.out.println("On swap "+task1+" et "+task2);
        }

        Result result= new Result(instance,base.toSchedule(),Result.ExitCause.ProvedOptimal);
        return result;
    }
}
