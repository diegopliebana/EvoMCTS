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

mergeFiles = [] 

#TEVC_MCTS larger K
#filenames = ['TEVC_MCTS_100iter_K10_100r','TEVC_MCTS_500iter_K2_100r','TEVC_MCTS_1000iter_K1_100r','sampleMCTS_1000iter_100r']
#filenames = ['sampleMCTS_1000iter_100r_decay0.5','TEVC_MCTS_100iter_K10_100r_decay0.5']

#different reward discount/decay rates
#filenames = ['2014_10_31_r1000_sampleMCTS_100iterK1_decay1.00_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.99_H8-30','2014_11_02_r1000_sampleMCTS_decay0.90','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.50_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.00_H8-30']
#filenames = ['2014_10_31_r1000_sampleMCTS_100iterK1_decay1.00_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.99_H8-30','2014_10_31_r1000_sampleMCTS_100iterK1_decay0.50_H8-30']
#filenames = ['2014_11_02_r1000_TEVCMCTS_newFitness_decay0.50','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.90','2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_11_02_r1000_TEVCMCTS_newInverseFitness_decay0.80']
#filenames = ['2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.80_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#TEVC_MCTS different fitness functions
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_30_newInverseFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30']
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.50_H1-50','2014_10_31_r1000_TEVCMCTS_decay0.50_fitnessZero','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#BEST: sampleMCTS vs TEVC_MCTS
#filenames = ['2014_11_02_r1000_sampleMCTS_decay0.90','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80']


#2014_11_14 final measurements for the TEVC paper, leftRight
filenames = ['2014_11_04 sampleMCTS','2014_11_04 sampleMCTS (1-8)','2014_11_04 TEVCMCTS handtuned','2014_11_04 TEVCMCTS handtuned (1-8)','2014_11_04 TEVCMCTS random','2014_11_04 TEVCMCTS random (1-8)','2014_11_04 TEVCMCTS one+one','2014_11_04 TEVCMCTS u+one','2014_11_04 TEVCMCTS bandit20']
#filenames = ['2014_11_04 TEVCMCTS bandit','2014_11_04 TEVCMCTS bandit10','2014_11_04 TEVCMCTS bandit20']
mergeFiles = [[0,1],[2,3],[4,5]]

#mergmergeFiles = [[0,1]]eFiles = []     #don't merge any
#mergeFiles = [[0,1]]   #example: merge SECOND filename into FIRST
    #define which pairs of files should be merged into one (see above example). The merge copies only the first 8 values from the second list into the first list, the second list is then discarded from the results and filename arrays

analysis_type = 0
    # 0 - average date (raw input)
    # 1 - optimality of date (e.g., optimal number of steps divided by average number of steps) -- requires setting optimal_performance
    # 2 - percentage of optimal samples -- requires setting optimal_performance
optimal_performance = 9
    # leftRight - 9
    # circle - 13


###-- circle
#filenames = ['2014_10_31_r100_sampleMCTS_decay0.99','2014_10_31_r100_TEVCMCTS_decay0.50']



#Create a figure
fig = pylab.figure()

#Add a subplot (Grid of plots 1x1, adding plot 1)
ax = fig.add_subplot(111)  

#Init memory structures
averages = [[] for x in xrange(len(filenames))] 
std_devs = [[] for x in xrange(len(filenames))] 
std_errs = [[] for x in xrange(len(filenames))] 
roll_depth = [[] for x in xrange(len(filenames))] 

for j in xrange(len(filenames)):

    filename = filenames[j]

    #datafile = '../circle/' + filename + '.csv'
    datafile = '../leftright/' + filename + '.csv'
    #datafile = '../tomConsoleRuns/' + filename + '.txt'    #leftRight (Tom)

    print 'loading', datafile
    r = pylab.loadtxt(datafile, comments='#', delimiter=',')

    if analysis_type == 1:
        r = optimal_performance / r
    elif analysis_type == 2:
        r = (optimal_performance == r)
    
    NumberOfColumns = len(r[0])
    averages[j] = [0] * NumberOfColumns 
    std_devs[j] = [0] * NumberOfColumns
    std_errs[j] = [0] * NumberOfColumns
    roll_depth[j]  = [0] * NumberOfColumns
    
    for i in range(NumberOfColumns):
        roll_depth[j][i] = i+1
        averages[j][i] = np.average(r[:,i])
        std_devs[j][i] = np.std(r[:,i])
        std_errs[j][i] = np.std(r[:,i]) / np.sqrt(len(r[:,i]))

for m in mergeFiles:
    averages[m[0]][0:8] = averages[m[1]][0:8]
    std_devs[m[0]][0:8] = std_devs[m[1]][0:8]
    std_errs[m[0]][0:8] = std_errs[m[1]][0:8]
    roll_depth[m[0]][0:8] = roll_depth[m[1]][0:8]

for i in xrange(len(mergeFiles)):
    d = mergeFiles[i][1] - i
    del averages[d]
    del std_devs[d]
    del std_errs[d]
    del roll_depth[d]
    del filenames[d]

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

for i in xrange(len(averages)):
    errorfill(roll_depth[i],averages[i],std_errs[i])
    #errorfill(roll_depth[-7:],averages[-7:],std_errs[-7:]) #for leftright
    #errorfill(roll_depth[-5:],averages[-5:],std_errs[-5:]) #for circle
    #errorfill(roll_depth[-7:],averages[-7:],std_errs[-7:]) #for leftright
    #errorfill(roll_depth[-5:],averages[-5:],std_errs[-5:]) #for circle
    #errorfill(roll_depth[7:],averages[7:],std_errs[7:]) #for circle


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
