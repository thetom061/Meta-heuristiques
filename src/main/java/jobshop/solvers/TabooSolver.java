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
        //compteur d'itérations
        int k=0;
        //pour garder le meilleur makespan
        int makespan;
        //on crée le ressource order de la solution de base et la meilleur
        ResourceOrder base=new ResourceOrder(baseSolver.solve(instance,deadline).schedule.get());
        ResourceOrder meilleur=base;

        //on remplit Taboo, initialement aucune permutation est Taboo
        for (int i=0;i<instance.numTasks;i++){
            for (int j=0;j< instance.numTasks;j++){
                Taboo[i][j]=0;
            }
        }

        //on boucle sur les voisins
        while (k<maxiter) {
            //on incrémente le nombre d'itération
            k++;
            //On choisit le meilleur swap non taboo
            //On utilise les swaps pour gérer les taches qui sont échangés plus facilement
            List<Nowicki.Swap> swaps=new Nowicki().allSwaps(base);
//            System.out.println("On a "+swaps.size()+" swaps possibles");
            //si aucun swap n'est possible on a pas de voisin donc on retourne le résultat
            if (swaps.isEmpty()){
                return new Result(instance,meilleur.toSchedule(),Result.ExitCause.Blocked);
            }
            //pour choisir le meilleur voisin
            Nowicki.Swap bestswap = null;

            makespan=Integer.MAX_VALUE;
            for (Nowicki.Swap currentswap : swaps) {
                //On vérifie si on a le droit d'utiliser le swap
                Task try1=base.getTaskOfMachine(currentswap.machine, currentswap.t1);

                Task try2=base.getTaskOfMachine(currentswap.machine, currentswap.t2);
//                System.out.println("Swipe en question: "+try1+" et "+try2);
//                System.out.println(Taboo[try2.job*instance.numTasks+ try2.task][try1.job*instance.numTasks+ try1.task]);
//                System.out.println(Taboo[try1.job*instance.numTasks+ try1.task][try2.job*instance.numTasks+ try2.task]);
                //si swipe pas Taboo alors on vérifie si le makespan est meilleur que celui actuel
                if (Taboo[try1.job*instance.numTasks+ try1.task][try2.job*instance.numTasks+ try2.task]<=k && Taboo[try2.job*instance.numTasks+ try2.task][try1.job*instance.numTasks+ try1.task]<=k) {
//                    System.out.println("on Rentre");


                    //On choppe le meilleur swap
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);
                    //si le swipe est réalisable on update le makespan si il est meilleur que celui actuel
                    if (!base.toSchedule().isEmpty()) {
                        if (makespan > base.toSchedule().get().makespan()) {
                            makespan = base.toSchedule().get().makespan();
                            bestswap = currentswap;
                        }
                    }
                    //on unapply le swap
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);
                }else{
                    //si la solution est taboo mais qu'elle a un meilleur makespan on change meilleur
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);
                    if (!base.toSchedule().isEmpty()){
                        if (base.toSchedule().get().makespan()<meilleur.toSchedule().get().makespan()){
                            meilleur=base;
                        }
                    }
                    //on remet la solution normal
                    base.swapTasks(currentswap.machine, currentswap.t1, currentswap.t2);

                }

            }
            //on a pas de meilleurs swap donc on retourne le résultat
            if (bestswap==null){
                return new Result(instance,meilleur.toSchedule(),Result.ExitCause.Blocked);
            }
            //on rajoute du temps dans taboo pour les task swapper
            Task task1=base.getTaskOfMachine(bestswap.machine, bestswap.t1);
            Task task2=base.getTaskOfMachine(bestswap.machine, bestswap.t2);
            //On utilise les identifiants des tasks permutés en indice
            Taboo[task1.job*instance.numTasks+ task1.task][task2.job*instance.numTasks+ task2.task]=dureeTaboo+k;
//            System.out.println("update : "+Taboo[task1.job*instance.numTasks+ task1.task][task2.job*instance.numTasks+ task2.task]);
            //on applique le meilleur swap pour aller dans la prochaine boucle
            base.swapTasks(bestswap.machine, bestswap.t1, bestswap.t2);
            if (meilleur.toSchedule().get().makespan()>base.toSchedule().get().makespan()){
                meilleur=base;
            }
//            System.out.println("On swap "+task1+" et "+task2);
        }

        Result result= new Result(instance,meilleur.toSchedule(),Result.ExitCause.ProvedOptimal);
        return result;
    }
}
