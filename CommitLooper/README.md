# CommitLooper

## How to use

### Requirements

To use this script you will need:
 - `Git` installed on your host
 - An executable _jar_ of `SmellDetector`.
 - A `CSV` file name containing 2 column entries (_name_, _url_)
    - where _name_ is the project name set in the output.
    - where _url_ is the repository path on GitHub

### Usage

#### Configuration

The `config.sh` script holds the configuration used by both `ProjectLooper` and `CommitLooper` scripts.

#### ProjectLooper

Calling the script without argument `./projectLooper.sh` will try to use a local file `apps.csv`
containing the needed couple entries.

You can also specify a file to use by setting it in the program argument,
e.g. `./projectLooper.sh myFile.csv`

Finally, if you want to analyze a single project, you can input the couple directly
as the program arguments: `./projectLooper.sh myApp username/repository`

#### CommitLooper

If you want to analyze a single project that is already in your clone repository, 
you can directly call `./commitLooper.sh $projectName`.

You can also resume from a specific commit by calling `./commitLooper.sh $projectName $commitHash`.

## Tips

### Git clones

Since we are using a lot of connections to GitHub and some project
may be using submodules with https connections,
we hardly recommend to store your git credentials to Github when using this script.

The easiest way being to store your credentials into a `~/.git-credentials` file,
see the git documentation on this: https://git-scm.com/docs/git-credential-store

### My git log is not the same

We are using the topological order (`--topo-order`) to retrieve the commits in the same
configuration as the branches we use.

See the `git log` documentation: https://git-scm.com/docs/git-log#_commit_ordering

### I have too many processes!

We are currently not limiting the number of processes spawned by ProjectLooper,
so if you set 300 applications in your input file, you may find 300 running processes
at some point, watch out.