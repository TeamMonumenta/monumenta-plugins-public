import com.jcraft.jsch.*
import java.util.Optional

class AllowAnyHosts private constructor() {
	companion object {
		val instance = AllowAnyHosts()
	}
}

abstract class AuthProvider(val name: String, val shouldContinue: Boolean = false) {
	abstract fun tryProvider(jsch: JSch): Optional<String>
}

abstract class FileKeyProvider(name: String, shouldContinue: Boolean = false) : AuthProvider(name, shouldContinue) {
	abstract fun getIdentityFile(): String?

	override fun tryProvider(jsch: JSch): Optional<String> {
		val identityPath = getIdentityFile()
		val identityFilePassword = System.getenv("IDENTITY_FILE_PASSWORD") ?: ""

		if(identityPath == null)
			return Optional.of("no path specified")

		val identityFile = File(identityPath)

		if(!identityFile.exists()) {
			return Optional.of("cannot find identity file at '$identityPath'")
		}

		try {
			jsch.addIdentity(identityPath, identityFilePassword)
		} catch (e: Exception) {
			return Optional.of("failed to parse identity '$identityPath': ${e.message}")
		}

		return Optional.empty()
	}
}

class EnvKeyProvider : FileKeyProvider("IdentityFileEnv"){
	override fun getIdentityFile(): String? {
		return System.getenv("IDENTITY_FILE")
	}
}

class OpenSSHProvider(private val keyName: String) : FileKeyProvider("\$HOME/.ssh/$keyName", true){
	override fun getIdentityFile(): String? {
		return File(System.getProperty("user.home")).resolve(".ssh").resolve(keyName).path
	}
}

class SSHAgentKeyProvider: AuthProvider("SSHAgent") {
	override fun tryProvider(jsch: JSch): Optional<String> {
		if(System.getenv("SSH_AUTH_SOCK") == null) {
			return Optional.of("missing env variable SSH_AUTH_SOCK")
		}

		try {
			jsch.identityRepository = AgentIdentityRepository(SSHAgentConnector())
			return Optional.empty()
		} catch (e: Exception) {
			return Optional.of("failed to use SSHAgentConnector: ${e.message}")
		}
	}
}

class PageantKeyProvider : AuthProvider("Pageant") {
	override fun tryProvider(jsch: JSch): Optional<String> {
		if(!System.getProperty("os.name").toLowerCase().contains("win")) {
			return Optional.of("pageant can only be used on windows")
		}

		try {
			jsch.identityRepository = AgentIdentityRepository(PageantConnector())
			return Optional.empty()
		} catch (e: Exception) {
			return Optional.of("failed to use PageantConnector: ${e.message}")
		}
	}
}

class RemoteConfig(private val name: String) {
	@Input
	lateinit var host: String

	@Input
	var port: Int = 22

	@Input
	var timeout: Int = 0

	@Input
	var user: String? = null

	@Input
	lateinit var knownHosts: Any

	@Input
	lateinit var auth: Array<AuthProvider>

	val allowAnyHosts = AllowAnyHosts.instance

	override fun toString(): String {
		if(user != null)
			return "$name[$user@$host:$port]"
		return "$name[$host:$port]"
	}
}

class SessionHandler(private val remote: RemoteConfig) {
	private val jsch = JSch()
	private val session: Session

	private fun resolveHostKey(knownHosts: Any) {
		when (knownHosts) {
			is File -> {
				TODO("implement File")
			}
			is AllowAnyHosts -> {
				session.setConfig("StrictHostKeyChecking", "no")
				println("[ssh/warn]: Host key checking is off. It may be vulnerable to man-in-the-middle attacks.")
			}
			else -> throw IllegalArgumentException("knownHosts must be file, collection of files, or allowAnyHosts")
		}
	}

	init {
		println("[ssh/debug]: Attempting to connect to remote $remote")
		session = jsch.getSession(remote.user, remote.host, remote.port)

		resolveHostKey(remote.knownHosts)

		println("[ssh/debug]: Using the following authentication attempts:")
		var found = false

		for (auth in remote.auth) {
			var res: Optional<String>

			try {
				res = auth.tryProvider(jsch)
			} catch (e: Exception) {
				println("FAIL - ${auth.name}: ${e.message}")
				continue
			}

			if (res.isEmpty) {
				println("USING - ${auth.name}")
				found = true
				if (!auth.shouldContinue) {
					break
				}
			} else {
				println("SKIP - ${auth.name}: ${res.get()}")
			}
		}

		if(!found)
			throw RuntimeException("Exhausted authentication methods, check logs!")

		session.connect(remote.timeout)
	}

	fun execute(commandLine: String) {
		println("[ssh/debug]: Executing command '$commandLine' on $remote")
		val chan = session.openChannel("exec") as ChannelExec
		chan.setCommand(commandLine)
		chan.connect(remote.timeout)
		// TODO: stop ignoring output
	}

	fun put(from: File, to: String) {
		println("[ssh/debug]: Copying '$from' -> '$to' on $remote")
		val chan = session.openChannel("sftp") as ChannelSftp
		chan.connect()
		chan.put(from.path, to)
	}
}

class RunHandler {
	fun session(remote: RemoteConfig, closure: Action<SessionHandler>) {
		closure.execute(SessionHandler(remote))
	}
}

class Service {
	fun run(closure: Action<RunHandler>) {
		closure.execute(RunHandler())
	}
}

class SshPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		target.extensions.add("ssh", Service())
		target.extensions.add("remotes", createRemoteContainer(target))
	}

	private fun createRemoteContainer(project: Project): NamedDomainObjectContainer<RemoteConfig> {
		val remotes = project.container<RemoteConfig>()
		return remotes
	}
}

apply {
	plugin<SshPlugin>()
}
