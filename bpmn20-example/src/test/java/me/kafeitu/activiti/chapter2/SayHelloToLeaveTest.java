package me.kafeitu.activiti.chapter2;

import org.activiti.engine.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SayHelloToLeaveTest {

    @Test
    public void testStartProcess() throws Exception {
        //创建流程引擎
        ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .buildProcessEngine();

        // 部署流程定义文件
        RepositoryService repositoryService = processEngine.getRepositoryService();
        String bpmnFileName = "me/kafeitu/activiti/helloworld/SayHelloToLeave.bpmn";
        repositoryService
                .createDeployment()
                .addClasspathResource(bpmnFileName)
                .deploy();

        // 验证已部署流程定义
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery().singleResult();
        assertEquals("SayHelloToLeave", processDefinition.getKey());

        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applyUser", "employee1");
        variables.put("days", 3);
        //这里传入了Map类型的变量，这样Activiti启动流程时会将Map中的数据存入数据库
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "SayHelloToLeave", variables);
        assertNotNull(processInstance);
        //getId()方法获取的是流程实例在数据库中的id，getProcessDefinitionId()，SayHelloToLeave=版本号:流程在数据库中的id
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("pid=" + processInstance.getId() + ", pdid="
                + processInstance.getProcessDefinitionId());

        TaskService taskService = processEngine.getTaskService();
        Task taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();
        assertNotNull(taskOfDeptLeader);
        assertEquals("领导审批", taskOfDeptLeader.getName());

        //claim [kleɪm] 声明 这行的意思表示该任务归leaderUser所有
        taskService.claim(taskOfDeptLeader.getId(), "leaderUser");
        variables = new HashMap<String, Object>();
        variables.put("approved", true);
        taskService.complete(taskOfDeptLeader.getId(), variables);

        taskOfDeptLeader = taskService.createTaskQuery()
                .taskCandidateGroup("deptLeader").singleResult();
        assertNull(taskOfDeptLeader);

        HistoryService historyService = processEngine.getHistoryService();
        long count = historyService.createHistoricProcessInstanceQuery().finished()
                .count();
        assertEquals(1, count);
    }
}