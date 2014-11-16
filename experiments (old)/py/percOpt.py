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
#filenames = ['2014_11_02_r1000_TEVCMCTS_newFitness_decay0.50','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.90','2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_11_02_r1000_TEVCMCTS_newInverseFitness_decay0.80']
#filenames = ['2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.80_H8-30','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#TEVC_MCTS different fitness functions
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_30_newInverseFitness_TEVC_MCTS_100iterK1_decay0.99_H1-50','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.99_H8-30']
#filenames = ['2014_10_30_newFitness_TEVC_MCTS_100iterK1_decay0.50_H1-50','2014_10_31_r1000_TEVCMCTS_decay0.50_fitnessZero','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.50_H8-30']

#BEST: sampleMCTS vs TEVC_MCTS
#filenames = ['2014_11_02_r1000_sampleMCTS_decay0.90','2014_10_31_r1000_TEVC_MCTS_100iterK1_decay0.90_H8-30','2014_11_02_r1000_TEVCMCTS_newFitness_decay0.80']

###-- circle
#filenames = ['2014_10_31_r100_sampleMCTS_decay0.99','2014_10_31_r100_TEVCMCTS_decay0.50']
filenames = ['CIRCLE_SampleMCTS','CIRCLE1_11','CIRCLE1_U1','CIRCLE1_Rnd','CIRCLE1_HT','CIRCLE1_Bandit']

OPTIMAL = 13


#Create a figure
fig = pylab.figure()

#Add a subplot (Grid of plots 1x1, adding plot 1)
ax = fig.add_subplot(111)  

for filename in filenames:

    datafile = '../circle/perc_opt_100it_1000rep_1-100-step5_2feat/' + filename + '.csv'
    #datafile = '../leftright/' + filename + '.csv'
    #datafile = '../tomConsoleRuns/' + filename + '.txt'    #leftRight (Tom)

    print 'loading', datafile
    r = pylab.loadtxt(datafile, comments='#', delimiter=',')
    
    NumberOfColumns = len(r[0])
    optimalRuns = [0] * NumberOfColumns
    
    for i in range(NumberOfColumns):
        numRows = len(r[:,i])
        for j in range(numRows):
            if(r[:,i][j] == OPTIMAL):
                optimalRuns[i]+=1
    
        optimalRuns[i] /= float(numRows)
        optimalRuns[i] *= 100

    #Plot everything in this subplot.
    ax.plot(range(NumberOfColumns),optimalRuns,'-',linewidth=2.0)


plt.legend(filenames, 
           shadow=True, fancybox=True) 


rangeN=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
labels=['1','5','10','15','20','25','30','35','40','45','50','55','60','65','70','75','80','85','90','95','100']
plt.xticks(rangeN, labels)

plt.ylim([0,200])

#Titles and labels
#plt.title('Heuristic estimation: 8 routes')
plt.xlabel("Rollout length", fontsize=16)#
plt.ylabel("Percentage optimal plays", fontsize=16)

#plt.ylim([5,11])


#Save to file.
fig.savefig("../circle/perc_opt_100it_1000rep_1-100-step5_2feat/circle_perc_opt_100it_1000rep_1-100-step5_2feat.pdf")

# And show it:
plt.show()
