Akka Jenkins Infra
==================

Dependencies
------------

```
gem install bundler
bundle install
# install chef-dk: https://downloads.chef.io/chefdk
chef gem install knife-solo

sudo easy_install pip
# install littlechef: https://github.com/tobami/littlechef
pip install littlechef

berks vendor
```

Then prepare the lifflechef file:

```
mv littlechef.cfg-example littlechef.cfg
```

And fill it in with potential secrets etc.

Preparing a node for cheffing
-------------

```
fix node:cassandra-01 deploy_chef
```

Adding a new akka node automatically
------------------------------------

```
./gcloud-new-akka-node.sh akka-node-002 "[\"10.132.0.7\", \"10.132.0.8\"]"
```

Cooking nodes
-------------

Executing scripts on a given node:

```
fix node:moxie-a0 [role:specific-role]
```

**Cooking all nodes for the Akka team:**

```
# check which ones belong to akka_team:
fix list_nodes_with_role:akka_team

# cook their run_lists:
fix nodes_with_role:akka_team
```

Read more here: https://github.com/tobami/littlechef

Rsync problems
--------------
Sometimes adds too many weird options to ssh used in rsync and fixing in
`$ vim ~/.chefdk/gem/ruby/2.1.0/gems/knife-solo-0.5.1/lib/chef/knife/solo_cook.rb` is required,
as shown here: https://github.com/ktoso/knife-solo/commit/78d47efe6d0b1399ae5da3a8cc5c6dd05ef10dfc
