package searchengine.web_crawling;

import searchengine.dto.NodePage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class RecursiveWebCrawling extends RecursiveAction
{
    private final PageParser parser;
    private final NodePage nodePage;

    public RecursiveWebCrawling(PageParser parser, NodePage nodePage) {
        this.parser = parser;
        this.nodePage = nodePage;
    }


    @Override
    protected void compute() {
        List<RecursiveWebCrawling> allTasks = new ArrayList<>();
        nodePage.setPath(nodePage.getPrefix() + nodePage.getSuffix());

        parser.startPageParser(nodePage);

        for (String refOnChildPage : nodePage.getRefOnChildSet()) {
            NodePage nodePageChild = new NodePage();
            nodePageChild.setPrefix((nodePage.getPrefix()))
                    .setSuffix(refOnChildPage)
                    .setSiteId(nodePage.getSiteId())
                    .setTimeBetweenRequest(nodePage.getTimeBetweenRequest());

            RecursiveWebCrawling task = new RecursiveWebCrawling(parser, nodePage);
            try {
                Thread.sleep(nodePage.getTimeBetweenRequest());
                task.fork();
                allTasks.add(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            allTasks.forEach(ForkJoinTask::join);
            if(getPool().getActiveThreadCount() == 1
                && getPool().getQueuedTaskCount() == 0
                && getPool().getQueuedSubmissionCount() == 0) {
                getPool().shutdown();
            }


        }


    }
}
