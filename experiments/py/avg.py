import pylab
import numpy as np
import matplotlib.pyplot as plt
import operator
from matplotlib.pyplot import errorbar

def errorfill(x, y, yerr, color=None, alpha_fill=0.3, ax=None):
    ax = ax if ax is not None else plt.gca()
    if color is None:
        color = ax._get_lines.color_cycle.next()
    if np.isscalar(yerr) or len(yerr) == len(y):
        ymin = [a - b for a,b in zip(y, yerr)] 
        ymax = [sum(pair) for pair in zip(y, yerr)] 
    elif len(yerr) == 2:
        ymin, ymax = yerr

    ax.plot(x, y, color=color)
    print ymin
    print y
    print ymax
    ax.fill_between(x, ymax, ymin, color=color, alpha=alpha_fill)
    
###-- Take the data --###

###-- leftRight
#datafile = 'inout/sampleH3.csv'

#filenames = ['SampleMCTS', 'FEMCTS-HT', 'FEMCTS-RND', 'FEMCTS-11', 'FEMCTS-U1']
#filenames = ['SampleMCTS', 'SampleMCTS-NODF']
#filenames = ['FEMCTS-HT', 'FEMCTS-HT-NODF']

#filenames = ['TEVC_MCTS_100iter_K10_100r','TEVC_MCTS_500iter_K2_100r','TEVC_MCTS_1000iter_K1_100r','sampleMCTS_1000iter_100r']
#filenames = ['sampleMCTS_100iter_100r_decay0.5','TEVC_MCTS_100iter_K1_100r_decay0.5']
#filenames = ['sampleMCTS_1000iter_100r_decay0.5','TEVC_MCTS_100iter_K10_100r_decay0.5']
#filenames = ['FEMCTS-Bandit-random-k1_2feat', 'FEMCTS-Bandit-random-k1_1feat', 'sampleMCTS']

#filenames = ['FEMCTS_BanditK10_1feat_Proper', 'sampleMCTS_1000iter_100r']

###-- leftRight (Tom)

#TEVC_MCTS larger K
#filenames = ['TEVC_MCTS_100iter_K10_100r','TEVC_MCTS_500iter_K2_100r','TEVC_MCTS_1000iter_K1_100r','sampleMCTS_1000iter_100r']
#filenames = ['sampleMCTS_1000iter_100r_decay0.5','TEVC_MCTS_100iter_K10_100r_decay0.5']

#different reward discount/decay rates
#filenames = ['2014_10_31_r1000_sampleMCTS_100iterK1_decay1.00_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.99_H8-30','2014_11_02_r1000_sampleMCTS_decay0.90','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.50_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.00_H8-30']
#filenames = ['2014_10_31_r1000_sampleMCTS_100iterK1_decay1.00_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.99_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.50_H8-30']
filenames = ['2014_11_02_r1000_TEVCMCTS_newFitness_decay0.50','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.90','2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_11_02_r1000_TEVCMCTS_newInverseFitness_decay0.80']
#filenames = ['2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.80_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#TEVC_MCTS different fitness functions
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_30_newInverseFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30']
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.50_H1-50','2014_10_31_r1000_TEVCMCTS_decay0.50_fitnessZero','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#BEST: sampleMCTS vs TEVC_MCTS
#filenames = ['2014_11_02_r1000_sampleMCTS_decay0.90','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80']

###-- circle
#filenames = ['2014_10_31_r100_sampleMCTS_decay0.99','2014_10_31_r100_TEVCMCTS_decay0.50']



#Create a figure
fig = pylab.figure()

#Add a subplot (Grid of plots 1x1, adding plot 1)
ax = fig.add_subplot(111)  

for filename in filenames:

    #datafile = '../circle/' + filename + '.csv'
    #datafile = '../leftright/' + filename + '.csv'
    datafile = '../tomConsoleRuns/' + filename + '.txt'    #leftRight (Tom)

    print 'loading', datafile
    r = pylab.loadtxt(datafile, comments='#', delimiter=',')
    
    NumberOfColumns = len(r[0])
    averages = [0] * NumberOfColumns 
    std_devs = [0] * NumberOfColumns
    std_errs = [0] * NumberOfColumns
    roll_depth  = [0] * NumberOfColumns
    
    for i in range(NumberOfColumns):
        roll_depth[i] = i+1
        averages[i] = np.average(r[:,i])
        std_devs[i] = np.std(r[:,i])
        std_errs[i] = np.std(r[:,i]) / np.sqrt(len(r[:,i]))
    


#Plot everything in this subplot.
#ax.plot(roll_depth,averages,'-',linewidth=2.0)
#ax.plot(gen,best,'--',linewidth=2.0)
#ax.plot(iterations,average,'-',linewidth=2.0)

#errorbar(iterations,average)
#errorbar(iterations,average,yerr=stderr)

# plot all points without error bars
#ax.plot(iterations, fitness)
# plot errorbars for every 50th point
#errorbar(roll_depth,averages,std_errs, linestyle='None')

        
    #errorfill(roll_depth,averages,std_errs)
    #errorfill(roll_depth[-7:],averages[-7:],std_errs[-7:]) #for leftright
    #errorfill(roll_depth[-5:],averages[-5:],std_errs[-5:]) #for circle
    #errorfill(roll_depth[-7:],averages[-7:],std_errs[-7:]) #for leftright
    #errorfill(roll_depth[-5:],averages[-5:],std_errs[-5:]) #for circle
    errorfill(roll_depth[7:],averages[7:],std_errs[7:]) #for circle


##Add the legend
#plt.legend(('1+1','HT'), 
#           shadow=True, fancybox=True)

plt.legend(filenames, 
           shadow=True, fancybox=True) 

#Titles and labels
#plt.title('Heuristic estimation: 8 routes')
plt.xlabel("Rollout length", fontsize=16)#
plt.ylabel("Average time steps", fontsize=16)

#plt.ylim([5,11])


#Save to file.
fig.savefig("../leftright/FEMCTS_BanditK10_1feat_Proper.pdf")

# And show it:
plt.show()
