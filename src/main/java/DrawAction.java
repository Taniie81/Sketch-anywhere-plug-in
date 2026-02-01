import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class DrawAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // On crée la fenêtre
        DrawingDialog dialog = new DrawingDialog();
        // On l'affiche
        dialog.show();
    }
}