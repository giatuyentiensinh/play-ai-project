import models.SaveData;
import play.Application;
import play.GlobalSettings;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		new SaveData(0);
	}

}
