package io.github.dsh105.echopet.entity;

import io.github.dsh105.dshutils.Particle;
import io.github.dsh105.dshutils.logger.Logger;
import io.github.dsh105.echopet.EchoPet;
import io.github.dsh105.echopet.api.event.PetAttackEvent;
import io.github.dsh105.echopet.api.event.PetRideJumpEvent;
import io.github.dsh105.echopet.api.event.PetRideMoveEvent;
import io.github.dsh105.echopet.data.PetHandler;
import io.github.dsh105.echopet.entity.living.SizeCategory;
import io.github.dsh105.echopet.entity.living.pathfinder.PetGoalSelector;
import io.github.dsh105.echopet.entity.living.pathfinder.goals.PetGoalFloat;
import io.github.dsh105.echopet.entity.living.pathfinder.goals.PetGoalFollowOwner;
import io.github.dsh105.echopet.entity.living.pathfinder.goals.PetGoalLookAtPlayer;
import io.github.dsh105.echopet.menu.main.MenuOption;
import io.github.dsh105.echopet.menu.main.PetMenu;
import io.github.dsh105.echopet.util.MenuUtil;
import net.minecraft.server.v1_7_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;

public abstract class EntityPet extends EntityCreature implements EntityOwnable, IMonster {

    public boolean vnp;

    public EntityLiving goalTarget = null;
    protected Pet pet;

    protected int particle = 0;
    protected int particleCounter = 0;

    protected Field jump = null;
    protected double jumpHeight;
    protected float rideSpeed;

    public PetGoalSelector petGoalSelector;

    public EntityPet(World world) {
        super(world);
    }

    public EntityPet(World world, Pet pet) {
        super(world);
        try {
            this.pet = pet;
            this.pet.setCraftPet(this.getBukkitEntity());
            this.getBukkitEntity().setMaxHealth(pet.getPetType().getMaxHealth());
            this.setHealth((float) pet.getPetType().getMaxHealth());
            this.jumpHeight = EchoPet.getInstance().options.getRideJumpHeight(this.getPet().getPetType());
            this.rideSpeed = EchoPet.getInstance().options.getRideSpeed(this.getPet().getPetType());
            //https://github.com/Bukkit/mc-dev/blob/master/net/minecraft/server/EntityLiving.java#L1322-L1334
            this.jump = EntityLiving.class.getDeclaredField("bd");
            this.jump.setAccessible(true);
            setPathfinding();
        } catch (Exception e) {
            Logger.log(Logger.LogLevel.WARNING, "Error creating new EntityLivingPet.", e, true);
            this.remove(false);
        }
    }

    public Pet getPet() {
        return this.pet;
    }

    public Player getPlayerOwner() {
        return pet.getOwner();
    }

    @Override
    public String getOwnerName() {
        return this.getPlayerOwner().getName();
    }

    @Override
    public Entity getOwner() {
        return ((CraftPlayer) this.getPlayerOwner()).getHandle();
    }

    public Location getLocation() {
        return this.pet.getLocation();
    }

    public void setVelocity(Vector vel) {
        this.motX = vel.getX();
        this.motY = vel.getY();
        this.motZ = vel.getZ();
        this.velocityChanged = true;
    }

    public Random random() {
        return this.random;
    }

    public boolean attack(Entity entity) {
        return this.attack(entity, (float) this.getPet().getPetType().getAttackDamage());
    }

    public boolean attack(Entity entity, float f) {
        return this.attack(entity, DamageSource.mobAttack(this), f);
    }

    public boolean attack(Entity entity, DamageSource damageSource, float f) {
        PetAttackEvent attackEvent = new PetAttackEvent(this.getPet(), entity.getBukkitEntity(), damageSource, f);
        EchoPet.getInstance().getServer().getPluginManager().callEvent(attackEvent);
        if (!attackEvent.isCancelled()) {
            if (entity instanceof EntityPlayer) {
                if (!((Boolean) EchoPet.getInstance().options.getConfigOption("canAttackPlayers", false))) {
                    return false;
                }
            }
            return entity.damageEntity(damageSource, (float) attackEvent.getDamage());
        }
        return false;
    }

    public void setPathfinding() {
        try {
            this.petGoalSelector = new PetGoalSelector();
            this.getNavigation().b(true);

            petGoalSelector.addGoal("Float", new PetGoalFloat(this));
            petGoalSelector.addGoal("FollowOwner", new PetGoalFollowOwner(this, this.getSizeCategory().getStartWalk(getPet().getPetType()), this.getSizeCategory().getStopWalk(getPet().getPetType()), this.getSizeCategory().getTeleport(getPet().getPetType())));
            petGoalSelector.addGoal("LookAtPlayer", new PetGoalLookAtPlayer(this, EntityHuman.class, 8.0F));

        } catch (Exception e) {
            Logger.log(Logger.LogLevel.WARNING, "Could not add PetGoals to EntityLivingPet AI.", e, true);
        }
    }

    // EntityInsentient
    @Override
    public boolean bk() {
        return true;
    }

    @Override
    public abstract CraftPet getBukkitEntity();

    // Overriden from EntityInsentient - Most importantly overrides pathfinding selectors
    @Override
    protected void bn() {
        super.bn();
        ++this.aV;

        this.w();

        this.getEntitySenses().a();

        this.petGoalSelector.run();

        this.getNavigation().f();

        this.bp();

        this.getControllerMove().c();
        this.getControllerLook().a();
        this.getControllerJump().b();
    }

    // EntityInsentient
    public boolean a(EntityHuman human) {
        if (human.getBukkitEntity() == this.getPlayerOwner().getPlayer()) {
            if ((Boolean) EchoPet.getInstance().options.getConfigOption("pets." + this.getPet().getPetType().toString().toLowerCase().replace("_", " ") + ".interactMenu", true)) {
                ArrayList<MenuOption> options = MenuUtil.createOptionList(getPet().getPetType());
                int size = this.getPet().getPetType() == PetType.HORSE ? 18 : 9;
                PetMenu menu = new PetMenu(getPet(), options, size);
                menu.open(true);
            }
            return true;
        }
        return false;
    }

    public void setLocation(Location l) {
        this.setLocation(l.getX(), l.getY(), l.getZ(), l.getPitch(), l.getYaw());
        this.world = ((CraftWorld) l.getWorld()).getHandle();
    }

    public void teleport(Location l) {
        this.getPet().getCraftPet().teleport(l);
    }

    public void remove(boolean makeSound) {
        bukkitEntity.remove();
        makeSound(this.getDeathSound(), 1.0F, 1.0F);
    }

    public void onLive() {
        if (this.getPlayerOwner() == null || !this.getPlayerOwner().isOnline() || Bukkit.getPlayerExact(this.getPlayerOwner().getName()) == null) {
            PetHandler.getInstance().removePet(this.getPet(), true);
        }

        if (((CraftPlayer) this.getPlayerOwner()).getHandle().isInvisible() != this.isInvisible() && !this.vnp) {
            this.setInvisible(!this.isInvisible());
        }

        if (this.isInvisible()) {
            try {
                Particle.MAGIC_CRITIAL.sendToPlayer(this.getLocation(), this.getPlayerOwner());
                Particle.WITCH_MAGIC.sendToPlayer(this.getLocation(), this.getPlayerOwner());
            } catch (Exception e) {
            }
        }

        if (((CraftPlayer) this.getPlayerOwner()).getHandle().isSneaking() != this.isSneaking()) {
            this.setSneaking(!this.isSneaking());
        }

        if (((CraftPlayer) this.getPlayerOwner()).getHandle().isSprinting() != this.isSprinting()) {
            this.setSprinting(!this.isSprinting());
        }

        if (this.getPet().isHat()) {

            this.lastYaw = this.yaw = (this.getPet().getPetType() == PetType.ENDERDRAGON ? this.getPlayerOwner().getLocation().getYaw() - 180 : this.getPlayerOwner().getLocation().getYaw());
        }

        if (this.particle == this.particleCounter) {
            this.particle = 0;
            this.particleCounter = this.random.nextInt(50);
        } else {
            this.particle++;
        }

        if (this.getPlayerOwner().isFlying() && EchoPet.getInstance().options.canFly(this.getPet().getPetType())) {
            Location petLoc = this.getLocation();
            Location ownerLoc = this.getPlayerOwner().getLocation();
            Vector v = ownerLoc.toVector().subtract(petLoc.toVector());

            double x = v.getX();
            double y = v.getY();
            double z = v.getZ();

            Vector vo = this.getPlayerOwner().getLocation().getDirection();
            if (vo.getX() > 0) {
                x = x - 1.5;
            } else if (vo.getX() < 0) {
                x = x + 1.5;
            }
            if (vo.getZ() > 0) {
                z = z - 1.5;
            } else if (vo.getZ() < 0) {
                z = z + 1.5;
            }

            this.setVelocity(new Vector(x, y, z).normalize().multiply(0.3F));
        }
    }

    // EntityInsentient
    @Override
    public void e(float sideMot, float forwMot) {
        if (this.passenger == null || !(this.passenger instanceof EntityHuman)) {
            super.e(sideMot, forwMot);
            // https://github.com/Bukkit/mc-dev/blob/master/net/minecraft/server/EntityHorse.java#L914
            this.X = 0.5F;
            return;
        }
        EntityHuman human = (EntityHuman) this.passenger;
        if (human.getBukkitEntity() != this.getPlayerOwner().getPlayer()) {
            super.e(sideMot, forwMot);
            this.X = 0.5F;
            return;
        }

        this.X = 1.0F;

        this.lastYaw = this.yaw = this.passenger.yaw;
        this.pitch = this.passenger.pitch * 0.5F;
        this.b(this.yaw, this.pitch);
        this.aP = this.aN = this.yaw;

        sideMot = ((EntityLiving) this.passenger).be * 0.5F;
        forwMot = ((EntityLiving) this.passenger).bf;

        if (forwMot <= 0.0F) {
            forwMot *= 0.25F;
        }
        sideMot *= 0.75F;

        PetRideMoveEvent moveEvent = new PetRideMoveEvent(this.getPet(), forwMot, sideMot);
        EchoPet.getInstance().getServer().getPluginManager().callEvent(moveEvent);
        if (moveEvent.isCancelled()) {
            return;
        }

        this.i(this.rideSpeed);
        super.e(moveEvent.getSidewardMotionSpeed(), moveEvent.getForwardMotionSpeed());

        PetType pt = this.getPet().getPetType();
        if (jump != null) {
            if (EchoPet.getInstance().options.canFly(pt)) {
                try {
                    if (((Player) (human.getBukkitEntity())).isFlying()) {
                        ((Player) (human.getBukkitEntity())).setFlying(false);
                    }
                    if (jump.getBoolean(this.passenger)) {
                        PetRideJumpEvent rideEvent = new PetRideJumpEvent(this.getPet(), this.jumpHeight);
                        EchoPet.getInstance().getServer().getPluginManager().callEvent(rideEvent);
                        if (!rideEvent.isCancelled()) {
                            this.motY = 0.5F;
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.LogLevel.WARNING, "Failed to initiate Pet Flying Motion for " + this.getPlayerOwner().getName() + "'s Pet.", e, true);
                }
            } else if (this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        PetRideJumpEvent rideEvent = new PetRideJumpEvent(this.getPet(), this.jumpHeight);
                        EchoPet.getInstance().getServer().getPluginManager().callEvent(rideEvent);
                        if (!rideEvent.isCancelled()) {
                            this.motY = rideEvent.getJumpHeight();
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.LogLevel.WARNING, "Failed to initiate Pet Jumping Motion for " + this.getPlayerOwner().getName() + "'s Pet.", e, true);
                }
            }
        }
    }

    // EntityInsentient
    @Override
    protected String t() {
        return this.getIdleSound();
    }

    // EntityInsentient
    @Override
    protected String aU() {
        return this.getDeathSound();
    }

    protected abstract String getIdleSound(); //idle sound

    protected abstract String getDeathSound(); //death sound

    public abstract SizeCategory getSizeCategory();

    // Entity
    @Override
    public void h() {
        super.h();
        //this.C();
        try {
            onLive();
        } catch (Exception e) {
        }
    }

    // EntityLiving
    @Override
    protected void c() {
        super.c();
        try {
            initDatawatcher();
        } catch (Exception e) {
        }
    }

    // Entity
    @Override
    protected void a(int i, int j, int k, Block block) {
        super.a(i, j, k, block);
        try {
            makeStepSound(i, j, k, block);
        } catch (Exception e) {
        }
    }

    protected void makeStepSound(int i, int j, int k, Block block) {
        this.makeStepSound();
    }

    protected void initDatawatcher() {
    }

    protected void makeStepSound() {
    }
}