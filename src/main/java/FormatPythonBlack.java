import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class FormatPythonBlack extends AnAction {
    private static String BLACK_PATH_PROPERTY_KEY = "com.truework.black_formatter.black_path";

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            runBlack(event);
        } catch (IOException | InterruptedException e) {
            displayErrorMessage(event, e.getMessage());
        }
    }

    private void runBlack(AnActionEvent event) throws IOException, InterruptedException {
        String path = getBlackPath(event);

        Editor editor = event.getData(PlatformDataKeys.EDITOR);

        if (editor == null) {
            throw new RuntimeException("no active editor");
        }

        Process black = Runtime.getRuntime().exec(new String[]{
                path, "-"
        });

        Document document = editor.getDocument();
        black.getOutputStream().write(document.getText().getBytes());
        black.getOutputStream().close();

        black.waitFor();

        if (black.exitValue() != 0) {
            String error = streamToString(black.getErrorStream());
            throw new RuntimeException(error);
        }

        String result = streamToString(black.getInputStream());

        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        WriteCommandAction.runWriteCommandAction(project, () ->
                document.setText(result)
        );
    }

    private String streamToString(InputStream stream) {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(stream));
        return stdInput.lines().collect(Collectors.joining("\n"));
    }

    private String getBlackPath(AnActionEvent event) {
        return "/Users/jordan/.virtualenvs/truework-frontend-api/bin/black";
//        PropertiesComponent properties = getSettings(event);
//        return properties.getValue(BLACK_PATH_PROPERTY_KEY);
    }

    private PropertiesComponent getSettings(AnActionEvent event) {
        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        return PropertiesComponent.getInstance(project);
    }

    private void displayErrorMessage(AnActionEvent event, String message) {
        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(PlatformDataKeys.PROJECT.getData(event.getDataContext()));


        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("Black: " + message,
                        MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getSouthEastOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }

}
