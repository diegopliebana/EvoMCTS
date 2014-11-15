"""
Demo of spines offset from the axes (a.k.a. "dropped spines").
"""
import pylab
import numpy as np
import matplotlib.pyplot as plt


fig, ax = plt.subplots()

filenames = ['HitsOnGoal10','HitsOnGoal9','HitsOnGoal8',
             'HitsOnGoal7', 'HitsOnGoal6','HitsOnGoal5',
             'HitsOnGoal4','HitsOnGoal3', 'HitsOnGoal2',
             'HitsOnGoal1']

#filenames = ['SampleMCTS-Dist10','SampleMCTS-Dist9','SampleMCTS-Dist8',
#             'SampleMCTS-Dist7', 'SampleMCTS-Dist6','SampleMCTS-Dist5',
#             'SampleMCTS-Dist4', 'SampleMCTS-Dist3','SampleMCTS-Dist2',
#             'SampleMCTS-Dist1']

filenames = ['sampleMCTS_controllers.sampleMCTS.Agent_Dist14','sampleMCTS_controllers.sampleMCTS.Agent_Dist13','sampleMCTS_controllers.sampleMCTS.Agent_Dist12',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist11','sampleMCTS_controllers.sampleMCTS.Agent_Dist10','sampleMCTS_controllers.sampleMCTS.Agent_Dist9',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist8','sampleMCTS_controllers.sampleMCTS.Agent_Dist7','sampleMCTS_controllers.sampleMCTS.Agent_Dist6',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist5','sampleMCTS_controllers.sampleMCTS.Agent_Dist4','sampleMCTS_controllers.sampleMCTS.Agent_Dist3',
             'sampleMCTS_controllers.sampleMCTS.Agent_Dist2','sampleMCTS_controllers.sampleMCTS.Agent_Dist1']

filenames = ['tevcMCTS_TEVC_MCTS.Agent_Dist14','tevcMCTS_TEVC_MCTS.Agent_Dist13','tevcMCTS_TEVC_MCTS.Agent_Dist12',
             'tevcMCTS_TEVC_MCTS.Agent_Dist11','tevcMCTS_TEVC_MCTS.Agent_Dist10','tevcMCTS_TEVC_MCTS.Agent_Dist9',
             'tevcMCTS_TEVC_MCTS.Agent_Dist8','tevcMCTS_TEVC_MCTS.Agent_Dist7','tevcMCTS_TEVC_MCTS.Agent_Dist6',
             'tevcMCTS_TEVC_MCTS.Agent_Dist5','tevcMCTS_TEVC_MCTS.Agent_Dist4','tevcMCTS_TEVC_MCTS.Agent_Dist3',
             'tevcMCTS_TEVC_MCTS.Agent_Dist2','tevcMCTS_TEVC_MCTS.Agent_Dist1']

averages = [[] for filename in filenames]
std_err = [[] for filename in filenames]
f = 0

for filename in filenames:

    
    #datafile = '../circle/' + filename + '.csv'
    #datafile = '../leftright/fixedPos/tevcMCTS_lr_2feat_100rx100it/' + filename + '.txt'
    datafile = '../circle/fixedPos/' + filename + '.txt'
    #datafile = '../tomConsoleRuns/' + filename + '.txt'

    print 'loading', datafile
    r = pylab.loadtxt(datafile, comments='#', delimiter=',')
    
    NumberOfColumns = len(r[0])
    averages[f] = [0] * NumberOfColumns 
    std_err[f] = [0] * NumberOfColumns 
    roll_depth  = [0] * NumberOfColumns
    
    for i in range(NumberOfColumns):
        roll_depth[i] = i+1
        real_average = np.average(r[:,i])
        averages[f][i] = 1 - real_average
        
        std_dev = np.std(r[:,i])
        std_err[f][i] = std_dev / np.sqrt(len(r[:,i]))
        
        if(real_average < 0.2):
            cVal = 'black'
        else:
            cVal = 'white'
            
        text = round(real_average,2)
        #text = round(2*std_dev,2)
            
        #text = round(real_average,2), "(", round(std_err[f][i],2), ")"
        #text = round(std_err[f][i],2)
        #text = std_err[f][i]
        
        ax.text(i, f, text, color=cVal, va='center', ha='center')
    
    f+=1





#ax.imshow(averages, cmap=plt.cm.gray, interpolation='nearest')
ax.imshow(averages, cmap=plt.cm.gray, interpolation='nearest')
ax.set_title('% hits on goal')

# Move left and bottom spines outward by 10 points
ax.spines['left'].set_position(('outward', 10))
ax.spines['bottom'].set_position(('outward', 10))
# Hide the right and top spines
ax.spines['right'].set_visible(False)
ax.spines['top'].set_visible(False)
# Only show ticks on the left and bottom spines
ax.yaxis.set_ticks_position('left')
ax.xaxis.set_ticks_position('bottom')

#rangeN=[0,1,2,3,4,5,6,7,8,9]
#labels=['10','9','8','7','6','5','4','3','2','1']
rangeN=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14]
labels=['14','13','12','11','10','9','8','7','6','5','4','3','2','1']
plt.yticks(rangeN, labels)


#rangeN=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14]
#labels=['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15']
rangeN=[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
labels=['1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20']
plt.xticks(rangeN, labels)

    


plt.xlabel("Rollout length", fontsize=16)#
plt.ylabel("Distance to goal", fontsize=16)

plt.show()

fig.savefig("../circle/fixedPos/tevcMCTS_circle_1feat_100rx1000it.pdf")
