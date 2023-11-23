package tests;

import org.apache.maven.project.MavenProject;
import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
check in generated code:
	package-info.java
		should have @NullableByDefault annotation (or provided by params)
	data structure classes
		should have @NotNull (where it is required)

*/
public class NonnullTest extends RunXJC2Mojo {

	@Override
	protected File getGeneratedDirectory() {
		return new File(getBaseDir(), "target/generated-sources");
	}

	@Override
	public File getSchemaDirectory() {
		return new File(getBaseDir(), "src/test/resources");
	}

	@Override
	protected void configureMojo(AbstractXJC2Mojo mojo) {
		super.configureMojo(mojo);
		mojo.setProject(new MavenProject());
		mojo.setForceRegenerate(true);
		mojo.setExtension(true);
	}

	@Override
	public List<String> getArgs() {
		final List<String> args = new ArrayList<>(super.getArgs());
		args.add("-XJsr305Annotations");
		args.add("-XJsr305Annotations:generateListItemNonnull=true");
//		args.add("-XJsr305Annotations:defaultNullableClass=tests.DummyDefaultNullable");
//		args.add("-XJsr305Annotations:generateDefaultNullable=false");
//		args.add("-XJsr305Annotations:nonnullClass=tests.DummyNonnull");
		return args;
	}
}
