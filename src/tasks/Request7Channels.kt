package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import contributors.logRepos
import contributors.logUsers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }

        val allUsers = mutableListOf<User>()
        val channel = Channel<List<User>>()

        for (repo in repos) {
            launch {
                val users = service
                    .getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                channel.send(users)
            }
        }

        repeat(repos.size) { index ->
            val users = channel.receive()
            allUsers += users
            val isCompleted = index == repos.lastIndex
            updateResults(allUsers.aggregate(), isCompleted)
        }
    }
}
