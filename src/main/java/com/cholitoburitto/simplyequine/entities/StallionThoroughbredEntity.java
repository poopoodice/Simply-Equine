package com.cholitoburitto.simplyequine.entities;

import com.cholitoburitto.simplyequine.init.ModEntityTypes;
import com.cholitoburitto.simplyequine.util.RegistryHandler;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import software.bernie.geckolib.animation.builder.AnimationBuilder;
import software.bernie.geckolib.animation.controller.AnimationController;
import software.bernie.geckolib.animation.controller.EntityAnimationController;
import software.bernie.geckolib.entity.IAnimatedEntity;
import software.bernie.geckolib.event.AnimationTestEvent;
import software.bernie.geckolib.manager.EntityAnimationManager;


public class StallionThoroughbredEntity extends AbstractHorseEntity implements IAnimatedEntity, IEntityAdditionalSpawnData {

    private EatGrassGoal eatGrassGoal;
    private int exampleTimer;
    private EntityAnimationManager manager = new EntityAnimationManager();
    private AnimationController controller = new EntityAnimationController(this, "moveController", 20, this::animationPredicate);
    private int textureType = 0;

    /**
     * Generates a random texture type
     */
    public StallionThoroughbredEntity(EntityType<? extends AbstractHorseEntity> type, World worldIn) {
        super(type, worldIn);
        registerAnimationControllers();
        //let's say we have 5 types of texture
        setTextureType(rand.nextInt(16));
    }

    /**
     * Set texture type for this entity
     *
     * @param textureType texture type
     */
    public StallionThoroughbredEntity(EntityType<? extends AbstractHorseEntity> type, World worldIn, int textureType) {
        super(type, worldIn);
        setTextureType(textureType);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.eatGrassGoal = new EatGrassGoal(this);
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.fromItems(RegistryHandler.THATCH.get()), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(5, this.eatGrassGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(this.getModifiedMaxHealth());
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.getModifiedMovementSpeed());
        this.getAttribute(JUMP_STRENGTH).setBaseValue(this.getModifiedJumpStrength());
    }

    @Override
    protected void updateAITasks() {
        this.exampleTimer = this.eatGrassGoal.getEatingGrassTimer();
        super.updateAITasks();
    }

    @Override
    public void livingTick() {
        if (this.world.isRemote) {
            this.exampleTimer = Math.max(0, this.exampleTimer - 1);
        }
        super.livingTick();
    }

    @Override
    public boolean processInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getHeldItem(hand);
        boolean flag = !itemstack.isEmpty();
        if (flag && itemstack.getItem() instanceof SpawnEggItem) {
            return super.processInteract(player, hand);
        } else {
            if (!this.isChild()) {
                if (this.isTame() && player.isSecondaryUseActive()) {
                    this.openGUI(player);
                    return true;
                }

                if (this.isBeingRidden()) {
                    return super.processInteract(player, hand);
                }
            }

            if (flag) {
                if (this.handleEating(player, itemstack)) {
                    if (!player.abilities.isCreativeMode) {
                        itemstack.shrink(1);
                    }

                    return true;
                }

                if (itemstack.interactWithEntity(player, this, hand)) {
                    return true;
                }

                if (!this.isTame()) {
                    this.makeMad();
                    return true;
                }

                boolean flag1 = !this.isChild() && !this.isHorseSaddled() && itemstack.getItem() == Items.SADDLE;
                if (this.isArmor(itemstack) || flag1) {
                    this.openGUI(player);
                    return true;
                }
            }

            if (this.isChild()) {
                return super.processInteract(player, hand);
            } else {
                this.mountTo(player);
                return true;
            }
        }
    }
//canberiden

    @Override
    public boolean canMateWith(AnimalEntity otherAnimal) {
        if (otherAnimal == this) {
            return false;
        } else if (!(otherAnimal instanceof DonkeyEntity) && !(otherAnimal instanceof HorseEntity)) {
            return false;
        } else {
            return this.canMate() && otherAnimal.canMateWith(StallionThoroughbredEntity.this);
        }
    }

    @Override
    public AgeableEntity createChild(AgeableEntity ageable) {
        StallionThoroughbredEntity stallionThoroughbredEntity;
        stallionThoroughbredEntity = ModEntityTypes.STALLION_THOROUGHBRED_ENTITY.get().create(this.world);
        this.setOffspringAttributes(ageable, stallionThoroughbredEntity);
        return stallionThoroughbredEntity;
    }

    @Override
    public EntityAnimationManager getAnimationManager() {
        return manager;
    }

    private <E extends StallionThoroughbredEntity> boolean animationPredicate(AnimationTestEvent<E> event) {
        if (event.isWalking()) {
            controller.setAnimation(new AnimationBuilder().addAnimation("simply_equine.animation.walk")
                    .addAnimation("simply_equine.animation.walk", true));
            return true;
        } else {
            return false;
        }
    }

    private void registerAnimationControllers() {
        manager.addAnimationController(controller);
    }

    public void setTextureType(int textureType) {
        //making sure we have the texture type (betwee 0 and 4 inclusive)
        textureType = Math.min(Math.max(0, textureType), 16);
        this.textureType = textureType;
    }

    public int getTextureType() {
        return textureType;
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putInt("texturetype", getTextureType());
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        setTextureType(compound.getInt("texturetype"));
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeInt(getTextureType());
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        setTextureType(additionalData.readInt());
    }
}

