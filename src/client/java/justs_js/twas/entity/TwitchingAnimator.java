package justs_js.twas.entity;

import com.google.common.collect.ImmutableSet;
import justs_js.twas.TWASModClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Function;

public class TwitchingAnimator {
    private boolean animateMovement;
    private float size;

    private static Animation MOVEMENT;
    private static Animation JUMP;
    private final Set<Animation> animations;

    public void tick(Vec3 pos, Vec3 oldPos) {
        MOVEMENT.tick((float)(Math.abs(pos.x() - oldPos.x()) + Math.abs(pos.z() - oldPos.z())), size);
        JUMP.tick((float)(pos.y() - oldPos.y()), size);
    }

    public TwitchingAnimator(boolean movement, float size) {
        this.animateMovement = movement;
        this.size = size;
        this.animations = ImmutableSet.of(MOVEMENT, JUMP);
    }

    public void setAnimateMovement(boolean animateMovement) {
        this.animateMovement = animateMovement;
    }

    public boolean getAnimateMovement() {
        return animateMovement;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public CompoundTag getAnimatedPoseState() {
        CompoundTag poseTag = new CompoundTag();
        ListTag poseBodyTag = new ListTag();

        Triplet<Float> triplet = accumulate(Animation::getBody);
        poseBodyTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseBodyTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseBodyTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("Body", poseBodyTag);

        ListTag poseLeftLegTag = new ListTag();
        triplet = accumulate(Animation::getLeftLeg);
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseLeftLegTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("LeftLeg", poseLeftLegTag);

        ListTag poseRightLegTag = new ListTag();
        triplet = accumulate(Animation::getRightLeg);
        poseRightLegTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseRightLegTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseRightLegTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("RightLeg", poseRightLegTag);

        ListTag poseLeftArmTag = new ListTag();
        triplet = accumulate(Animation::getLeftArm);
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseLeftArmTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("LeftArm", poseLeftArmTag);

        ListTag poseRightArmTag = new ListTag();
        triplet = accumulate(Animation::getRightArm);
        poseRightArmTag.add(FloatTag.valueOf(triplet.getFirst()));
        poseRightArmTag.add(FloatTag.valueOf(triplet.getSecond()));
        poseRightArmTag.add(FloatTag.valueOf(triplet.getThird()));
        poseTag.put("RightArm", poseRightArmTag);

        return poseTag;
    }

    private Triplet<Float> accumulate(Function<Animation, Triplet<Float>> function) {
        float f = 0;
        float s = 0;
        float t = 0;
        for (Animation animation : animations) {
            Triplet<Float> result = function.apply(animation);
            f += result.getFirst();
            s += result.getSecond();
            t += result.getThird();
        }
        return Triplet.of(f, s ,t);
    }

    private abstract class Animation {
        protected final float animationSpeed;
        protected final float animationMultiplier;
        protected final float animationMaxAngle;
        protected float animationAngle = 0f;
        protected float animationIntensity = 0;
        protected float animationState = 0;

        Animation(float speed, float multiplier, float maxAngle) {
            this.animationSpeed = speed;
            this.animationMultiplier = multiplier;
            this.animationMaxAngle = maxAngle;
        }

        void tick(float delta, float size) {}

        Triplet<Float> getRightLeg() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getLeftLeg() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getRightArm() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getLeftArm() {return Triplet.of(0f, 0f, 0f);}
        Triplet<Float> getBody() {return Triplet.of(0f, 0f, 0f);}
    }

    private static class Triplet<T> {
        private final T first;
        private final T second;
        private final T third;

        Triplet(T f, T s, T t) {
            this.first = f;
            this.second = s;
            this.third = t;
        }

        static <E> Triplet<E> of(E f, E s, E t) {
            return new Triplet<>(f, s ,t);
        }

        public T getFirst() {
            return first;
        }

        public T getSecond() {
            return second;
        }

        public T getThird() {
            return third;
        }
    }

    {
        MOVEMENT = new Animation(0.3f, 3f, 60f){
            @Override
            void tick(float delta, float size) {
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationAngle = (float) ((animationAngle + appliedSpeed) % (2 * Math.PI));
                animationIntensity = Math.clamp(delta * animationMultiplier, 0f, 1f);
                animationState = (float) Math.sin(animationAngle) * animationMaxAngle * animationIntensity;
                if (Float.compare(0f, animationIntensity) == 1) {
                    animationAngle = 0;
                }
            }
            @Override
            Triplet<Float> getLeftLeg() {return Triplet.of(animationState * -1, 0f, 0f);}
            @Override
            Triplet<Float> getRightLeg() {return Triplet.of(animationState, 0f, 0f);}
            @Override
            Triplet<Float> getLeftArm() {return Triplet.of(animationState, 0f, 0f);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(animationState * -1, 0f, 0f);}
        };

        JUMP = new Animation(2f, 3f, 30f){
            @Override
            void tick(float delta, float size) {
                float appliedSpeed = this.animationSpeed * 1 / size;
                animationIntensity = Math.clamp(Math.abs(delta) * animationMultiplier, 0f, 1f);
                animationAngle += appliedSpeed * Math.signum(delta);
                animationAngle = Math.clamp(animationAngle, 0, animationMaxAngle);
                animationState = animationAngle;
            }
            @Override
            Triplet<Float> getLeftLeg() {return Triplet.of(0f, 0f, animationState * -1);}
            @Override
            Triplet<Float> getRightLeg() {return Triplet.of(0f, 0f, animationState);}
            @Override
            Triplet<Float> getLeftArm() {return Triplet.of(0f, 0f, animationState * -1);}
            @Override
            Triplet<Float> getRightArm() {return Triplet.of(0f, 0f, animationState);}
        };
    }
}
