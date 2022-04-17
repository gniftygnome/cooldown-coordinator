![icon](./src/main/resources/assets/cooldown-coordinator/icon.png)

# Cooldown Coordinator
Fabric library to help mods coordinate item movement cooldowns

## What it Does
Cooldown Coordinator is a library mod intended to enable heterogeneous networks
of vanilla hoppers and item transferring modded block entities to move items
throughout the network in a vanilla-alike and consistent manner.  For example,
one goal is to allow sorters consisting of a mixture of vanilla and modded
hoppers, ducts, etc. to behave predictably.

## How Does it Do it?
The mod provides an interface to implement and a method to call when a cooldown
should be triggered.  The method may safely be called with any BlockEntity as
its argument.  When the target BE implements the interface, it will be notified
of the cooldown and can take whatever action it deems appropriate.  When this
mod is installed, descendants of HopperBlockEntity which do not override HBE's
insert() and innermost transfer() methods automatically implement the interface
and will coordinate with other implementing BEs.

## How to Use this Library 
There are some important details (f.e. how to know when a cooldown should be
triggered) which implementors need to get right in order for coordination to
work properly.  See the library's javadocs, or take a look at the
[wiki](https://github.com/gniftygnome/cooldown-coordinator/wiki).

## Future Goals
I would like this mod or something similar to be part of the Fabric API.  At
present, it is somewhere between an RFC and a WIP.  If it performs as intended
and there is interest from the Fabric community, I intend to maintain the mod
until (and beyond) it becomes part of Fabric or something better comes along.
